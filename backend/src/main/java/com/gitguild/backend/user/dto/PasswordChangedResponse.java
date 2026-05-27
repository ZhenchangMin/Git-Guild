package com.gitguild.backend.user.dto;

import java.time.OffsetDateTime;

public record PasswordChangedResponse(OffsetDateTime changedAt) {
}
