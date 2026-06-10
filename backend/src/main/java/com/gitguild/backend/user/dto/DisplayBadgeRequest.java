package com.gitguild.backend.user.dto;

/**
 * PUT /api/v1/users/me/display-badge — 设置或取消佩戴徽章。
 * badgeId 为 null 时取消佩戴。
 */
public class DisplayBadgeRequest {

    private String badgeId;

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }
}
