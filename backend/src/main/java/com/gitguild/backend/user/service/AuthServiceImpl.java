package com.gitguild.backend.user.service;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.security.TokenType;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.PasswordChangedResponse;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.TokenResponse;
import com.gitguild.backend.user.dto.UserResponse;
import com.gitguild.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        UserRole role = request.getRole() == null ? UserRole.BEGINNER : request.getRole();
        if (role == UserRole.ADMIN) {
            throw new BusinessException(
                    "FORBIDDEN",
                    HttpStatus.FORBIDDEN,
                    "公开注册不允许创建管理员账号",
                    "role=ADMIN");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_REGISTERED",
                    HttpStatus.CONFLICT,
                    "邮箱已被注册",
                    request.getEmail() + " 已存在对应账号");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    "USERNAME_ALREADY_TAKEN",
                    HttpStatus.CONFLICT,
                    "用户名已被占用",
                    "username=" + request.getUsername());
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsername(request.getAccount(), request.getAccount())
                .orElseThrow(this::invalidCredentials);
        ensureActive(user);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                List.of(user.getRole().name()),
                user.getTokenVersion());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUserId(),
                List.of(user.getRole().name()),
                user.getTokenVersion());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                UserResponse.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)
                || jwtTokenProvider.getTokenType(refreshToken) != TokenType.REFRESH) {
            throw new BusinessException("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "刷新令牌已过期", "请重新登录");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = findUser(userId);
        ensureActive(user);
        if (jwtTokenProvider.getTokenVersion(refreshToken) != user.getTokenVersion()) {
            throw new BusinessException("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "刷新令牌已过期", "请重新登录");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                List.of(user.getRole().name()),
                user.getTokenVersion());
        return new TokenResponse(accessToken, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = findUser(userId);
        ensureActive(user);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public PasswordChangedResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        ensureActive(user);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "邮箱或密码错误", "当前密码校验失败");
        }
        user.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new PasswordChangedResponse(OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        User user = findUser(userId);
        user.rotateTokenVersion();
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "用户不存在", "userId=" + userId));
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", HttpStatus.FORBIDDEN, "用户账号被禁用", "userId=" + user.getUserId());
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "邮箱或密码错误", "登录凭据校验失败");
    }
}
