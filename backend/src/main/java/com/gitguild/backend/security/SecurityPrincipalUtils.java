package com.gitguild.backend.security;

import com.gitguild.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public final class SecurityPrincipalUtils {

    private SecurityPrincipalUtils() {
    }

    public static Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "未登录或访问令牌无效");
        }
        return principal.userId();
    }
}
