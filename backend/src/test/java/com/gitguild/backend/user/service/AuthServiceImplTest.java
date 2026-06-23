package com.gitguild.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.security.TokenType;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.domain.UserStatus;
import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.ForgotPasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.ResetPasswordRequest;
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

    @Mock
    private PasswordResetCodeService passwordResetCodeService;

    @Mock
    private MailService mailService;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(SECRET, 3_600_000, 86_400_000);
        authService = new AuthServiceImpl(
                userRepository, passwordEncoder, jwtTokenProvider, passwordResetCodeService, mailService);
    }

    @Test
    void registerShouldCreateMaintainerWithEncodedPassword() {
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
        assertThat(response.role()).isEqualTo(UserRole.MAINTAINER);
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
        when(userRepository.findByEmailOrUsername("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest("alice@example.com", "GitGuild2026"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtTokenProvider.getTokenType(response.accessToken())).isEqualTo(TokenType.ACCESS);
        assertThat(jwtTokenProvider.getTokenType(response.refreshToken())).isEqualTo(TokenType.REFRESH);
        assertThat(response.user().userId()).isEqualTo(10001L);
    }

    @Test
    void loginShouldAcceptUsername() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        when(userRepository.findByEmailOrUsername("alice", "alice")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest("alice", "GitGuild2026"));

        assertThat(response.user().userId()).isEqualTo(10001L);
    }

    @Test
    void loginShouldRejectWrongPassword() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        when(userRepository.findByEmailOrUsername("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("alice@example.com", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void loginShouldRejectDisabledAccount() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.BEGINNER);
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmailOrUsername("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(user));

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
    void requestPasswordResetShouldIssueAndSendCodeWhenEmailExists() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.MAINTAINER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetCodeService.issue("alice@example.com")).thenReturn("123456");

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("alice@example.com");
        authService.requestPasswordReset(request);

        verify(passwordResetCodeService).issue("alice@example.com");
        verify(mailService).sendPasswordResetCode("alice@example.com", "123456");
    }

    @Test
    void requestPasswordResetShouldRejectUnregisteredEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@example.com");

        assertThatThrownBy(() -> authService.requestPasswordReset(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("EMAIL_NOT_REGISTERED");
        verifyNoInteractions(passwordResetCodeService);
        verifyNoInteractions(mailService);
    }

    @Test
    void resetPasswordShouldUpdateHashWhenCodeValid() {
        User user = user("alice@example.com", "GitGuild2026", UserRole.MAINTAINER);
        when(passwordResetCodeService.verifyAndConsume("alice@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ResetPasswordRequest request = resetRequest("alice@example.com", "123456", "GitGuild2026New");
        authService.resetPassword(request);

        assertThat(passwordEncoder.matches("GitGuild2026New", user.getPasswordHash())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void resetPasswordShouldRejectInvalidCode() {
        when(passwordResetCodeService.verifyAndConsume(eq("alice@example.com"), anyString())).thenReturn(false);

        ResetPasswordRequest request = resetRequest("alice@example.com", "000000", "GitGuild2026New");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_RESET_CODE");
        verify(userRepository, never()).save(any(User.class));
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

    private LoginRequest loginRequest(String account, String password) {
        LoginRequest request = new LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        request.setRemember(false);
        return request;
    }

    private ResetPasswordRequest resetRequest(String email, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode(code);
        request.setNewPassword(newPassword);
        return request;
    }

    private User user(String email, String rawPassword, UserRole role) {
        User user = new User("alice", email, passwordEncoder.encode(rawPassword), role);
        user.setUserId(10001L);
        return user;
    }
}
