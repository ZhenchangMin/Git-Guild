package com.gitguild.backend.user.dto;

import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.domain.UserStatus;
import java.time.OffsetDateTime;

public record UserResponse(
        Long userId,
        String username,
        String email,
        UserRole role,
        UserStatus status,
        String avatarUrl,
        String motto,
        String displayBadgeId,
        OffsetDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getAvatarUrl(),
                user.getMotto(),
                user.getDisplayBadgeId(),
                user.getCreatedAt());
    }
}
