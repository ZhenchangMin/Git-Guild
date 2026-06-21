package com.gitguild.backend.user.service;

import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.ForgotPasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.PasswordChangedResponse;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.ResetPasswordRequest;
import com.gitguild.backend.user.dto.TokenResponse;
import com.gitguild.backend.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    UserResponse getCurrentUser(Long userId);

    PasswordChangedResponse changePassword(Long userId, ChangePasswordRequest request);

    /** 忘记密码第一步：若邮箱已注册则生成验证码并发送邮件（邮箱不存在时静默处理）。 */
    void requestPasswordReset(ForgotPasswordRequest request);

    /** 忘记密码第二步：校验验证码并重置密码，成功后旧登录态全部失效。 */
    void resetPassword(ResetPasswordRequest request);

    void logout(Long userId);
}
