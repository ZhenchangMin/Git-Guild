package com.gitguild.backend.codehost.gitea;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitea")
public record GiteaProperties(String baseUrl, String token, String adminUsername) {
}
