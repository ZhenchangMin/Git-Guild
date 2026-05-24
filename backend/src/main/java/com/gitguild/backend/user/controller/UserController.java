package com.gitguild.backend.user.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.PasswordChangedResponse;
import com.gitguild.backend.user.dto.UserResponse;
import com.gitguild.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser(Authentication authentication) {
        return ApiResponse.success(authService.getCurrentUser(SecurityPrincipalUtils.currentUserId(authentication)));
    }

    @PatchMapping("/me/password")
    public ApiResponse<PasswordChangedResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.success(
                "密码已修改",
                authService.changePassword(SecurityPrincipalUtils.currentUserId(authentication), request));
    }
}
