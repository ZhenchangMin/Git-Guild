package com.gitguild.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitguild.backend.security.CurrentUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AssistantChatContextTest {

    @Test
    void shouldBuildAnonymousContextWhenAuthenticationIsMissing() {
        AssistantChatContext context = AssistantChatContext.from(" 怎么接任务？ ", " hall ", null);

        assertThat(context.message()).isEqualTo("怎么接任务？");
        assertThat(context.page()).isEqualTo("hall");
        assertThat(context.authenticated()).isFalse();
        assertThat(context.roleLabel()).isEqualTo("游客");
        assertThat(context.questStatuses()).isEmpty();
        assertThat(context.submissionStatuses()).isEmpty();
    }

    @Test
    void shouldExtractUserIdAndRolesFromCurrentUserPrincipal() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new CurrentUserPrincipal(2001L, List.of("ROLE_MAINTAINER", "ROLE_MAINTAINER"), 0),
                null);

        AssistantChatContext context = AssistantChatContext.from("打开工作台", "hall", authentication);

        assertThat(context.userId()).isEqualTo(2001L);
        assertThat(context.roles()).containsExactly("ROLE_MAINTAINER");
        assertThat(context.authenticated()).isTrue();
        assertThat(context.roleLabel()).isEqualTo("委托人");
    }
}
