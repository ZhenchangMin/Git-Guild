package com.gitguild.backend;

import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.config.AssistantRateLimitProperties;
import com.gitguild.backend.codehost.gitea.GiteaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({
        GiteaProperties.class,
        AssistantAiProperties.class,
        AssistantRateLimitProperties.class
})
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
