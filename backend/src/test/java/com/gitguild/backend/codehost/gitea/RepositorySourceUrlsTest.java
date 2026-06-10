package com.gitguild.backend.codehost.gitea;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RepositorySourceUrlsTest {

    @Test
    void hostExtractsFromHttpScpAndBlank() {
        assertThat(RepositorySourceUrls.host("https://gitea.com/ZhenchangMin/Operating-System.git"))
                .isEqualTo("gitea.com");
        assertThat(RepositorySourceUrls.host("http://localhost:3000/spike-admin/demo-repo"))
                .isEqualTo("localhost");
        assertThat(RepositorySourceUrls.host("git@github.com:owner/repo.git"))
                .isEqualTo("github.com");
        assertThat(RepositorySourceUrls.host("")).isEqualTo("");
    }

    @Test
    void isExternalSourceOnlyWhenBothHostsKnownAndDiffer() {
        // 不同 host → 外部，应迁移
        assertThat(RepositorySourceUrls.isExternalSource(
                "https://gitea.com/a/b", "http://localhost:3000")).isTrue();
        // 同 host（忽略端口/路径）→ 内部，不迁移
        assertThat(RepositorySourceUrls.isExternalSource(
                "http://localhost:3000/a/b", "http://localhost:3000")).isFalse();
        // 平台 baseUrl 未配置（如 test profile）→ 不可判定，一律按内部，绝不贸然迁移
        assertThat(RepositorySourceUrls.isExternalSource(
                "https://gitea.com/a/b", null)).isFalse();
        assertThat(RepositorySourceUrls.isExternalSource(
                "https://gitea.com/a/b", "")).isFalse();
    }

    @Test
    void deterministicRepoNameCombinesOwnerAndRepoSanitized() {
        assertThat(RepositorySourceUrls.deterministicRepoName(
                "https://gitea.com/ZhenchangMin/Operating-System.git"))
                .isEqualTo("ZhenchangMin-Operating-System");
        assertThat(RepositorySourceUrls.deterministicRepoName(
                "git@github.com:acme/My Repo.git"))
                .isEqualTo("acme-My-Repo");
    }
}
