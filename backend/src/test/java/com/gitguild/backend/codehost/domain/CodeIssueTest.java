package com.gitguild.backend.codehost.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeIssueTest {

    @Test
    void canCreateQuestShouldAcceptGiteaLowercaseOpenState() {
        CodeIssue issue = new CodeIssue(null, "1", "MVP issue", "open");

        assertThat(issue.canCreateQuest()).isTrue();
    }

    @Test
    void canCreateQuestShouldRejectClosedStateIgnoringCase() {
        CodeIssue issue = new CodeIssue(null, "1", "Closed issue", "closed");

        assertThat(issue.canCreateQuest()).isFalse();
    }
}
