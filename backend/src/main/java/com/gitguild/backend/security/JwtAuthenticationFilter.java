package com.gitguild.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 请求过滤器。
 *
 * <p>职责边界：
 * <ul>
 *   <li>从 Authorization 请求头中读取 Bearer Token。</li>
 *   <li>调用 JwtTokenProvider 校验和解析 Token。</li>
 *   <li>把认证结果写入 SecurityContext。</li>
 *   <li>不查询数据库，不调用业务 Service，不修改任何业务状态。</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            authenticateRequest(request, token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头中解析 Bearer Token。
     *
     * <p>合法格式示例：Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     */
    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private void authenticateRequest(HttpServletRequest request, String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            SecurityContextHolder.clearContext();
            log.debug("JWT 校验失败，请求路径：{}", request.getRequestURI());
            return;
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        if (authentication instanceof org.springframework.security.authentication.AbstractAuthenticationToken authToken) {
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
