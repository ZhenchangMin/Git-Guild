package com.gitguild.backend.growth.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BadgeResponse(List<BadgeItem> items) {

    public record BadgeItem(
            String badgeId,
            String name,
            String description,
            String condition,
            boolean earned,
            OffsetDateTime earnedAt,
            int progress,
            int target) {
    }
}
