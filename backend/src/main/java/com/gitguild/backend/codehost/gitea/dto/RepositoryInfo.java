package com.gitguild.backend.codehost.gitea.dto;

public record RepositoryInfo(
        long id,
        String name,
        String fullName,
        String defaultBranch,
        boolean empty,
        String htmlUrl) {
}
