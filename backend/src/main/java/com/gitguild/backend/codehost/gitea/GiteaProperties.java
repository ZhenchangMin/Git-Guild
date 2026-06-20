package com.gitguild.backend.codehost.gitea;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitea")
public record GiteaProperties(String baseUrl, String token, String adminUsername, String publicBaseUrl) {

    /**
     * 对外可访问的 Gitea 基址（浏览器用）。未配置时回退到内网 {@code baseUrl}。
     * 用于把仓库/Issue 的内网地址（如 127.0.0.1:3000）转换成用户可点开的外网地址。
     */
    @Override
    public String publicBaseUrl() {
        return (publicBaseUrl == null || publicBaseUrl.isBlank()) ? baseUrl : publicBaseUrl;
    }
}
