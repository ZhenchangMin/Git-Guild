package com.gitguild.backend.security;

import java.util.List;

public record CurrentUserPrincipal(
        Long userId,
        List<String> roles,
        int tokenVersion) {
}
