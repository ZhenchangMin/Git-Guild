package com.gitguild.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-for-gitguild-auth-module-32bytes";

    @Test
    void generateAccessTokenShouldBeValidAndRestoreAuthentication() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000, 86_400_000);

        String token = provider.generateAccessToken(10001L, List.of("BEGINNER"), 2);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(10001L);
        assertThat(provider.getTokenType(token)).isEqualTo(TokenType.ACCESS);
        assertThat(provider.getTokenVersion(token)).isEqualTo(2);

        Authentication authentication = provider.getAuthentication(token);
        assertThat(authentication.getPrincipal()).isInstanceOf(CurrentUserPrincipal.class);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_BEGINNER");
    }

    @Test
    void generateRefreshTokenShouldUseRefreshType() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000, 86_400_000);

        String token = provider.generateRefreshToken(10001L, List.of("ROLE_MAINTAINER"), 0);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getTokenType(token)).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void invalidTokenShouldReturnFalse() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000, 86_400_000);

        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void shortSecretShouldBeRejected() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", 3_600_000, 86_400_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("至少需要 32 字节");
    }
}
