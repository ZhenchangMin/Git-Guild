package com.gitguild.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

/**
 * Spring Security 统一配置。
 *
 * <p>本项目是前后端分离 REST API，因此采用无状态 JWT 鉴权：
 * <ul>
 *   <li>不使用 Session 保存登录状态。</li>
 *   <li>不启用表单登录和 HTTP Basic。</li>
 *   <li>登录、注册、Swagger 文档等公开接口放行。</li>
 *   <li>其他接口默认必须携带有效 JWT。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 显示开启 DORS 支持，让它读取下面的 corsConfigurationSource 配置
                .cors(cors -> {})
                // 前后端分离接口使用 JWT，不依赖浏览器 Cookie，因此关闭 CSRF。
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用默认表单登录，避免未登录时跳转 HTML 登录页。
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic，避免浏览器弹出默认认证框。
                .httpBasic(AbstractHttpConfigurer::disable)
                // JWT 是无状态认证，服务端不创建也不使用 Session。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "未登录或访问令牌无效");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "当前用户无权限访问该资源");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        // 浏览器跨域预检请求必须放行，否则前端无法正常调用带 Authorization 的接口。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 用户注册、登录、刷新令牌等认证入口允许匿名访问。
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Swagger / OpenAPI 文档允许匿名访问，方便开发阶段调试接口。
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Spring Boot 默认错误路径放行，避免异常处理再次被安全链拦截。
                        .requestMatchers("/error").permitAll()
                        // 其他业务接口默认都需要认证。
                        .anyRequest().authenticated())
                // JWT Filter 放在用户名密码过滤器之前，先尝试从请求头恢复登录状态。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    //新增CORS规则配置方法
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        // 允许前端 Vue 的默认本地开发地址访问
        config.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 密码加密器。
     *
     * <p>后续 AuthService 注册用户时应使用该 Bean 加密密码，登录时用它校验密码。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露 AuthenticationManager，后续登录接口可以用它执行用户名密码认证。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private static void writeJsonError(
            HttpServletResponse response,
            int status,
            String code,
            String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 加入了 timestamp 和 traceId
        String body = """
                {
                  "code": "%s",
                  "message": "%s",
                  "data": null,
                  "timestamp": "%s",
                  "traceId": null
                }
                """.formatted(code, message, java.time.OffsetDateTime.now());

        response.getWriter().write(body);
    }
}
