package com.gitguild.backend.user.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.dto.ChangePasswordRequest;
import com.gitguild.backend.user.dto.DisplayBadgeRequest;
import com.gitguild.backend.user.dto.PasswordChangedResponse;
import com.gitguild.backend.user.dto.UpdateProfileRequest;
import com.gitguild.backend.user.dto.UserResponse;
import com.gitguild.backend.user.repository.UserRepository;
import com.gitguild.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
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

    /**
     * 更新用户可编辑资料（目前支持 motto 座右铭）。
     * 传 null 的字段不做修改。
     */
    @PatchMapping("/me")
    @Transactional
    public ApiResponse<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        User user = findUser(userId);
        if (request.getMotto() != null) {
            user.setMotto(request.getMotto());
        }
        userRepository.save(user);
        return ApiResponse.success("资料已更新", UserResponse.from(user));
    }

    /**
     * 上传头像（multipart/form-data，字段名 file）。
     * MVP：将文件 Base64 编码后截断存 avatar_url；生产环境应存对象存储并存 URL。
     */
    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @Transactional
    public ApiResponse<UserResponse> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "头像文件不能为空", null);
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "头像文件不能超过 2MB", "size=" + file.getSize());
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "头像文件必须是图片格式", "contentType=" + contentType);
        }

        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        User user = findUser(userId);

        // MVP：转 Base64 Data URL 存储；后续替换为对象存储 URL
        try {
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            String dataUrl = "data:" + contentType + ";base64," + base64;
            user.setAvatarUrl(dataUrl);
        } catch (java.io.IOException e) {
            throw new BusinessException("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                    "头像上传失败", e.getMessage());
        }

        userRepository.save(user);
        return ApiResponse.success("头像已更新", UserResponse.from(user));
    }

    /**
     * 设置或取消佩戴徽章。badgeId 为 null 时取消佩戴。
     */
    @PutMapping("/me/display-badge")
    @Transactional
    public ApiResponse<UserResponse> setDisplayBadge(
            Authentication authentication,
            @RequestBody DisplayBadgeRequest request) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        User user = findUser(userId);
        user.setDisplayBadgeId(request.getBadgeId()); // null = 取消佩戴
        userRepository.save(user);
        return ApiResponse.success("徽章设置已更新", UserResponse.from(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "用户不存在", "userId=" + userId));
    }
}
