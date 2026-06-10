package com.gitguild.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌工具类。
 *
 * <p>职责边界：
 * <ul>
 *   <li>负责签发 JWT。</li>
 *   <li>负责校验 JWT 签名和过期时间。</li>
 *   <li>负责从 JWT 中还原 Spring Security 需要的 Authentication。</li>
 *   <li>不负责查询数据库，也不负责任何业务状态变更。</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final String ROLES_CLAIM = "roles";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥不能为空，请配置 jwt.secret 或 JWT_SECRET。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥长度至少需要 32 字节，避免 HMAC 签名强度不足。");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * 根据用户标识和角色集合签发访问令牌。
     *
     * @param subject 用户唯一标识，当前可使用 username 或 userId 字符串
     * @param roles 角色集合，例如 ROLE_BEGINNER、ROLE_MAINTAINER、ROLE_ADMIN
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(Long userId, Collection<String> roles, int tokenVersion, TokenType tokenType) {
        if (userId == null) {
            throw new IllegalArgumentException("JWT userId 不能为空");
        }
        if (tokenType == null) {
            throw new IllegalArgumentException("JWT tokenType 不能为空");
        }

        List<String> normalizedRoles = roles == null ? List.of() : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .toList();

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMillis(tokenType));

        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLES_CLAIM, normalizedRoles)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateAccessToken(Long userId, Collection<String> roles, int tokenVersion) {
        return generateToken(userId, roles, tokenVersion, TokenType.ACCESS);
    }

    public String generateRefreshToken(Long userId, Collection<String> roles, int tokenVersion) {
        return generateToken(userId, roles, tokenVersion, TokenType.REFRESH);
    }

    /**
     * 校验令牌是否有效。
     *
     * <p>只要签名错误、格式错误或令牌过期，JJWT 都会抛出 JwtException。
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 从令牌中取出用户标识。
     */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getSubject(token));
    }

    public TokenType getTokenType(String token) {
        return TokenType.valueOf(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public int getTokenVersion(String token) {
        Number version = parseClaims(token).get(TOKEN_VERSION_CLAIM, Number.class);
        return version == null ? 0 : version.intValue();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /**
     * 将 JWT 转换为 Spring Security 的 Authentication。
     *
     * <p>这里不查询数据库，只使用 JWT 中的 subject 和 roles 构造认证对象。
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        List<GrantedAuthority> authorities = extractAuthorities(claims);
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        int tokenVersion = getTokenVersion(token);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(userId, roles, tokenVersion);

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<GrantedAuthority> extractAuthorities(Claims claims) {
        Object rawRoles = claims.get(ROLES_CLAIM);
        if (!(rawRoles instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(role -> !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private long expirationMillis(TokenType tokenType) {
        return tokenType == TokenType.REFRESH ? refreshExpirationMs : expirationMs;
    }
}
