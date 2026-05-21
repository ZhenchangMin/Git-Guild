package com.gitguild.backend.codehost;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

/**
 * P4-008 Gitea API 最小闭环 Spike。
 *
 * 运行前提：
 *   1. 本地启动 Gitea（默认 http://localhost:3000）
 *   2. 在 Gitea 上创建一个测试仓库（owner/repo 可用），并确保 token 有该仓库的写权限
 *   3. 通过系统属性传入参数，例如：
 *      mvn test -pl backend -Dtest=GiteaSpikeTest -Dgitea.base-url=http://localhost:3000
 *              -Dgitea.token=YOUR_TOKEN -Dgitea.owner=youruser -Dgitea.repo=spike-repo
 *      或直接修改下面的默认值。
 *
 * 跑通标准（P4-008 完成标准）：
 *   [1] 仓库读取   GET  /api/v1/repos/{owner}/{repo}
 *   [2] Issue 列表 GET  /api/v1/repos/{owner}/{repo}/issues
 *   [3] 分支列表   GET  /api/v1/repos/{owner}/{repo}/branches
 *   [4] 上传文件   POST /api/v1/repos/{owner}/{repo}/contents/{path}  （产生一次 commit）
 *   [5] 创建 PR    POST /api/v1/repos/{owner}/{repo}/pulls
 *   [6] 合并 PR    POST /api/v1/repos/{owner}/{repo}/pulls/{index}/merge
 */
@Disabled("Requires a running Gitea instance — run manually with -Dtest=GiteaSpikeTest")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GiteaSpikeTest {

    // ── 配置（通过系统属性覆盖）─────────────────────────────────────────────
    private static final String BASE_URL = prop("gitea.base-url", "http://localhost:3000");
    private static final String TOKEN    = prop("gitea.token",    "REPLACE_ME");
    private static final String OWNER    = prop("gitea.owner",    "gitguild-spike");
    private static final String REPO     = prop("gitea.repo",     "spike-repo");

    // 测试间共享状态
    private static final String BRANCH = "spike-branch-" + System.currentTimeMillis();
    private static int prIndex = -1;

    private static RestClient client;

    @BeforeAll
    static void setup() {
        client = RestClient.builder()
                .baseUrl(BASE_URL + "/api/v1")
                .defaultHeader("Authorization", "token " + TOKEN)
                .defaultHeader("Content-Type", "application/json")
                .build();
        System.out.printf("%n=== Gitea Spike: %s | owner=%s | repo=%s ===%n%n", BASE_URL, OWNER, REPO);
    }

    // ── [1] 仓库读取 ─────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void t1_getRepository() {
        ResponseEntity<Map> resp = client.get()
                .uri("/repos/{owner}/{repo}", OWNER, REPO)
                .retrieve()
                .toEntity(Map.class);

        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        System.out.println("[1] 仓库读取 OK — name: " + resp.getBody().get("name"));
    }

    // ── [2] Issue 列表 ───────────────────────────────────────────────────────
    @Test
    @Order(2)
    void t2_listIssues() {
        ResponseEntity<Object[]> resp = client.get()
                .uri("/repos/{owner}/{repo}/issues?type=issues&state=open", OWNER, REPO)
                .retrieve()
                .toEntity(Object[].class);

        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        System.out.println("[2] Issue 列表 OK — count: " + resp.getBody().length);
    }

    // ── [3] 分支列表 ─────────────────────────────────────────────────────────
    @Test
    @Order(3)
    void t3_listBranches() {
        ResponseEntity<Object[]> resp = client.get()
                .uri("/repos/{owner}/{repo}/branches", OWNER, REPO)
                .retrieve()
                .toEntity(Object[].class);

        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        System.out.println("[3] 分支列表 OK — count: " + resp.getBody().length);
    }

    // ── [4] 上传文件（产生一次 commit，同时创建分支）────────────────────────
    @Test
    @Order(4)
    void t4_createFileOnNewBranch() {
        String content = Base64.getEncoder().encodeToString(
                ("# Spike commit\nCreated by GiteaSpikeTest at " + System.currentTimeMillis()).getBytes());

        Map<String, Object> body = Map.of(
                "message", "chore: spike commit from GiteaSpikeTest",
                "content", content,
                "new_branch", BRANCH
        );

        ResponseEntity<Map> resp = client.post()
                .uri("/repos/{owner}/{repo}/contents/spike-test.md", OWNER, REPO)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        Assertions.assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        System.out.println("[4] 上传文件 OK — branch: " + BRANCH);
    }

    // ── [5] 创建 PR ──────────────────────────────────────────────────────────
    @Test
    @Order(5)
    void t5_createPullRequest() {
        Map<String, Object> body = Map.of(
                "title",  "Spike PR — GiteaSpikeTest",
                "head",   BRANCH,
                "base",   "main",
                "body",   "Auto-created by GiteaSpikeTest for P4-008 validation."
        );

        ResponseEntity<Map> resp = client.post()
                .uri("/repos/{owner}/{repo}/pulls", OWNER, REPO)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        Assertions.assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        prIndex = (Integer) resp.getBody().get("number");
        System.out.println("[5] 创建 PR OK — number: " + prIndex);
    }

    // ── [6] 合并 PR ──────────────────────────────────────────────────────────
    @Test
    @Order(6)
    void t6_mergePullRequest() {
        Assertions.assertTrue(prIndex > 0, "PR index must be set by t5");

        Map<String, Object> body = Map.of(
                "Do", "merge",
                "merge_message_field", "Merged by GiteaSpikeTest"
        );

        ResponseEntity<Void> resp = client.post()
                .uri("/repos/{owner}/{repo}/pulls/{index}/merge", OWNER, REPO, prIndex)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Void.class);

        // Gitea 合并成功返回 200 或 204
        Assertions.assertTrue(
                resp.getStatusCode() == HttpStatus.OK || resp.getStatusCode() == HttpStatus.NO_CONTENT,
                "Expected 200 or 204, got: " + resp.getStatusCode());
        System.out.println("[6] 合并 PR OK — PR #" + prIndex + " merged");
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────
    private static String prop(String key, String defaultValue) {
        String v = System.getProperty(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
