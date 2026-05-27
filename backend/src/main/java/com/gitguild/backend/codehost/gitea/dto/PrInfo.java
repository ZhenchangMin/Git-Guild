package com.gitguild.backend.codehost.gitea.dto;

public record PrInfo(
        int number,
        String title,
        String state,
        boolean merged,
        String headBranch,
        String authorLogin) {
}
