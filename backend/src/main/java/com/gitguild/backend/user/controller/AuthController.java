package com.gitguild.backend.user.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ForgotPasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.ResetPasswordRequest;
import com.gitguild.backend.user.dto.TokenResponse;
import com.gitguild.backend.user.dto.UserResponse;
import com.gitguild.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("令牌已刷新", authService.refresh(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.success("验证码已发送，请查收邮件", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("密码重置成功，请使用新密码登录", null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(SecurityPrincipalUtils.currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
