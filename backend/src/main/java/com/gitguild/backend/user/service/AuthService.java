package com.gitguild.backend.user.service;

import com.gitguild.backend.user.dto.AuthResponse;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.LoginRequest;
import com.gitguild.backend.user.dto.PasswordChangedResponse;
import com.gitguild.backend.user.dto.RefreshTokenRequest;
import com.gitguild.backend.user.dto.RegisterRequest;
import com.gitguild.backend.user.dto.TokenResponse;
import com.gitguild.backend.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    UserResponse getCurrentUser(Long userId);

    PasswordChangedResponse changePassword(Long userId, ChangePasswordRequest request);

    void logout(Long userId);
}
