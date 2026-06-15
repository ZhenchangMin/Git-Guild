package com.gitguild.backend.codehost.gitea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 针对真实 Gitea 的 {@link GiteaAdapterImpl} 实跑验证（手动）。
 *
 * <p>与 {@code GiteaSpikeTest}（裸 RestClient，P4-008）不同，本测试构造真正的
 * {@link GiteaAdapterImpl}，经 Jackson 反序列化 + {@code toPrInfo} 验证新增的
 * {@code listPulls} 能从真 Gitea 正确读出 PR 字段（{@code base.ref} / {@code html_url} /
 * {@code merged} / {@code state}）。
 *
 * <p>平时跳过；手动开关运行：
 * <pre>
 *   ./mvnw test -Dgitea.live=true -Dtest=GiteaAdapterLiveTest \
 *     -Dgitea.base-url=http://localhost:3000 -Dgitea.token= \
 *     -Dgitea.owner=spike-admin -Dgitea.repo=spike-repo
 * </pre>
 *
 * <p>默认用空 token，借此同时验证 016 问题2：token 为空时只读公开仓库是否可行。
 */
@EnabledIfSystemProperty(named = "gitea.live", matches = "true")
class GiteaAdapterLiveTest {

    private static final String BASE  = prop("gitea.base-url", "http://localhost:3000");
    private static final String TOKEN = prop("gitea.token",    "");
    private static final String OWNER = prop("gitea.owner",    "spike-admin");
    private static final String REPO  = prop("gitea.repo",     "spike-repo");

    @Test
    void listPulls_readsRealPrFieldsFromLiveGitea() {
        GiteaAdapter adapter = new GiteaAdapterImpl(new GiteaProperties(BASE, TOKEN, OWNER, null));

        List<PrInfo> pulls = adapter.listPulls(OWNER, REPO);
        System.out.printf("%n=== listPulls(%s/%s) → %d PR ===%n", OWNER, REPO, pulls.size());
        pulls.forEach(p -> System.out.println("  " + p));

        assertFalse(pulls.isEmpty(), "spike-repo 在 state=all 下应至少有 1 个 PR");

        PrInfo pr = pulls.stream()
                .filter(p -> p.number() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 PR #1"));

        assertEquals("closed", pr.state(), "PR #1 应为 closed");
        assertTrue(pr.merged(), "PR #1 应为已合并");
        assertEquals("main", pr.baseBranch(), "目标分支应取自 base.ref");
        assertNotNull(pr.headBranch(), "源分支应取自 head.ref");
        assertTrue(pr.htmlUrl() != null && pr.htmlUrl().contains("/pulls/1"),
                "htmlUrl 应取自 html_url，实际=" + pr.htmlUrl());
        assertEquals("spike-admin", pr.authorLogin(), "作者应取自 user.login");
    }

    private static String prop(String key, String defaultValue) {
        String v = System.getProperty(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
