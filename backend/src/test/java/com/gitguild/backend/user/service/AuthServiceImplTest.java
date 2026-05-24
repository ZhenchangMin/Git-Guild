package com.gitguild.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.security.TokenType;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.domain.UserStatus;
import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.TokenResponse;
import com.gitguild.backend.user.dto.UserResponse;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "test-secret-for-gitguild-auth-module-32bytes";

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000, 86_400_000);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void registerShouldCreateBeginnerWithEncodedPassword() {
        RegisterRequest request = registerRequest("alice", "alice@example.com", "GitGuild2026", null);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10001L);
            return user;
        });

        UserResponse response = authService.register(request);

        assertThat(response.userId()).isEqualTo(10001L);
        assertThat(response.role()).isEqualTo(UserRole.BEGINNER);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("GitGuild2026");
        assertThat(passwordEncoder.matches("GitGuild2026", userCaptor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerShouldRejectDuplicatedEmail() {
        RegisterRequest request = registerRequest("alice", "alice@example.com", "GitGuild2026", null);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void registerShouldRejectDuplicatedUsername() {
        RegisterRequest request = registerRequest("alice", "alice@example.com", "GitGuild2026", null);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("USERNAME_ALREADY_TAKEN");
    }

    @Test
    void registerShouldRejectAdminRole() {
        RegisterRequest request = registerRequest("alice", "alice@example.com", "GitGuild2026", UserRole.ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokens() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest("alice@example.com", "GitGuild2026"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtTokenProvider.getTokenType(response.accessToken())).isEqualTo(TokenType.ACCESS);
        assertThat(jwtTokenProvider.getTokenType(response.refreshToken())).isEqualTo(TokenType.REFRESH);
        assertThat(response.user().userId()).isEqualTo(10001L);
    }

    @Test
    void loginShouldRejectWrongPassword() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("alice@example.com", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void loginShouldRejectDisabledAccount() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("alice@example.com", "GitGuild2026")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    void refreshShouldIssueNewAccessToken() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        String refreshToken = jwtTokenProvider.generateRefreshToken(10001L, java.util.List.of("BEGINNER"), user.getTokenVersion());
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        when(userRepository.findById(10001L)).thenReturn(Optional.of(user));

        TokenResponse response = authService.refresh(request);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtTokenProvider.getTokenType(response.accessToken())).isEqualTo(TokenType.ACCESS);
    }

    @Test
    void refreshShouldRejectAccessToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(10001L, java.util.List.of("BEGINNER"), 0);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(accessToken);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void changePasswordShouldUpdateHashAndRotateTokenVersion() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("GitGuild2026");
        request.setNewPassword("GitGuild2026New");
        when(userRepository.findById(10001L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.changePassword(10001L, request);

        assertThat(passwordEncoder.matches("GitGuild2026New", user.getPasswordHash())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void logoutShouldRotateTokenVersion() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        when(userRepository.findById(10001L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.logout(10001L);

        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    private RegisterRequest registerRequest(String username, String email, String password, UserRole role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setRemember(false);
        return request;
    }

    private User user(String email, String rawPassword, UserRole role) {
        User user = new User("alice", email, passwordEncoder.encode(rawPassword), role);
        user.setUserId(10001L);
        return user;
    }
}
