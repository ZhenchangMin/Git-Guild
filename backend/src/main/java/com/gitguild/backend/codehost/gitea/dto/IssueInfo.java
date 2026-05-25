package com.gitguild.backend.codehost.gitea.dto;

public record IssueInfo(
        long number,
        String title,
        String state,
        String htmlUrl) {
}
