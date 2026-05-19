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

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥不能为空，请配置 jwt.secret 或 JWT_SECRET。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥长度至少需要 32 字节，避免 HMAC 签名强度不足。");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 根据用户标识和角色集合签发访问令牌。
     *
     * @param subject 用户唯一标识，当前可使用 username 或 userId 字符串
     * @param roles 角色集合，例如 ROLE_BEGINNER、ROLE_MAINTAINER、ROLE_ADMIN
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(String subject, Collection<String> roles) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject 不能为空");
        }

        // 生成 Token 时，遍历所有角色，强行在前面拼接 “ROLE_” 字符串
        List<String> normalizedRoles = roles == null ? List.of() : roles.stream()
                .filter(Object::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_")) ? role "ROLE_" + role
 
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claim(ROLES_CLAIM, roles == null ? List.of() : roles)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
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

    /**
     * 将 JWT 转换为 Spring Security 的 Authentication。
     *
     * <p>这里不查询数据库，只使用 JWT 中的 subject 和 roles 构造认证对象。
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        List<GrantedAuthority> authorities = extractAuthorities(claims);

        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
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
}
