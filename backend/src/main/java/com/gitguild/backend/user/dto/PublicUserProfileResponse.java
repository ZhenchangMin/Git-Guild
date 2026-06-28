package com.gitguild.backend.user.dto;

import com.gitguild.backend.user.domain.User;
import java.time.OffsetDateTime;

/**
 * 其他成员可见的公开身份资料，不包含邮箱、角色和账号状态。
 */
public record PublicUserProfileResponse(
        Long userId,
        String username,
        String avatarUrl,
        String motto,
        String displayBadgeId,
        OffsetDateTime createdAt) {

    public static PublicUserProfileResponse from(User user) {
        return new PublicUserProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getMotto(),
                user.getDisplayBadgeId(),
                user.getCreatedAt());
    }
}
