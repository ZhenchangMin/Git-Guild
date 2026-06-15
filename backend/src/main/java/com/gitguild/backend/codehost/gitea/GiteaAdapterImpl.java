package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.FileCommitInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * {@link GiteaAdapter} 的 HTTP 实现，隐藏以下复杂度：
 * <ul>
 *   <li>Gitea REST API 的 snake_case 字段名映射到 Java 记录类型</li>
 *   <li>Jackson 将 JSON integer 反序列化为 {@code Integer}（非 {@code Long}）的类型差异</li>
 *   <li>{@code head.ref}（裸分支名）与 {@code head.label}（"{owner}:{branch}" 格式）的区别</li>
 *   <li>{@code merged} 字段为 boolean 而非可空时间戳的 Gitea 特有行为</li>
 * </ul>
 *
 * <p>使用系统级 admin token 认证，读写统一。token 通过 {@link GiteaProperties}
 * 从环境变量注入；token 为空时应用可启动，但所有调用将收到 401。
 *
 * <p><b>边界错误模式：</b>Gitea 返回 4xx 或网络不可达时，经 {@link #execute} 统一转换为
 * 带业务码的 {@link BusinessException}（{@code CODE_HOST_RESOURCE_NOT_FOUND} /
 * {@code CODE_HOST_UNAVAILABLE}），兑现已知问题清单 P4-016 问题1 的修复承诺。
 */
@Component
public class GiteaAdapterImpl implements GiteaAdapter {

    private final RestClient client;

    public GiteaAdapterImpl(GiteaProperties props) {
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl() + "/api/v1")
                .defaultHeader("Authorization", "token " + props.token())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public RepositoryInfo getRepository(String owner, String repo) {
        Map body = execute(() -> client.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo);
        return toRepositoryInfo(body);
    }

    @Override
    public List<IssueInfo> listIssues(String owner, String repo) {
        Map[] body = execute(() -> client.get()
                .uri("/repos/{owner}/{repo}/issues?type=issues&state=open&limit=50", owner, repo)
                .retrieve()
                .body(Map[].class), "repo=" + owner + "/" + repo + " issues");
        if (body == null) return List.of();
        return Arrays.stream(body).map(this::toIssueInfo).toList();
    }

    @Override
    public PrInfo getPullRequest(String owner, String repo, int prNumber) {
        Map body = execute(() -> client.get()
                .uri("/repos/{owner}/{repo}/pulls/{index}", owner, repo, prNumber)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo + " pull #" + prNumber);
        return toPrInfo(body);
    }

    @Override
    public List<BranchInfo> listBranches(String owner, String repo) {
        Map[] body = execute(() -> client.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .retrieve()
                .body(Map[].class), "repo=" + owner + "/" + repo + " branches");
        if (body == null) return List.of();
        return Arrays.stream(body).map(this::toBranchInfo).toList();
    }

    @Override
    public List<PrInfo> listPulls(String owner, String repo) {
        Map[] body = execute(() -> client.get()
                .uri("/repos/{owner}/{repo}/pulls?state=all", owner, repo)
                .retrieve()
                .body(Map[].class), "repo=" + owner + "/" + repo + " pulls");
        if (body == null) return List.of();
        return Arrays.stream(body).map(this::toPrInfo).toList();
    }

    @Override
    public IssueInfo createIssue(String owner, String repo, String title, String body) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("title", title);
        if (body != null && !body.isBlank()) {
            reqBody.put("body", body);
        }
        Map result = execute(() -> client.post()
                .uri("/repos/{owner}/{repo}/issues", owner, repo)
                .body(reqBody)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo + " create issue");
        return toIssueInfo(result);
    }

    @Override
    public BranchInfo createBranch(String owner, String repo, String newBranchName, String oldBranchName) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("new_branch_name", newBranchName);
        if (oldBranchName != null && !oldBranchName.isBlank()) {
            reqBody.put("old_branch_name", oldBranchName);
        }
        Map body = execute(() -> client.post()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .body(reqBody)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo + " create branch " + newBranchName);
        return toBranchInfo(body);
    }

    @Override
    public RepositoryInfo createRepository(String name, String description) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("name", name);
        reqBody.put("private", false);
        reqBody.put("default_branch", "main");
        reqBody.put("auto_init", true);
        if (description != null && !description.isBlank()) {
            reqBody.put("description", description);
        }
        Map body = execute(() -> client.post()
                .uri("/user/repos")
                .body(reqBody)
                .retrieve()
                .body(Map.class), "create repo name=" + name);
        return toRepositoryInfo(body);
    }

    @Override
    public RepositoryInfo migrateRepository(
            String cloneAddr, String repoName, String description, boolean withMetadata, String authToken) {
        String owner = resolveTokenOwner();
        // 幂等：目标仓库已存在则直接复用，避免重复迁移与 409
        RepositoryInfo existing = getRepositoryOrNull(owner, repoName);
        if (existing != null) {
            return existing;
        }

        String service = detectService(cloneAddr);
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("clone_addr", cloneAddr);
        reqBody.put("repo_owner", owner);
        reqBody.put("repo_name", repoName);
        reqBody.put("mirror", false);
        reqBody.put("private", false);
        reqBody.put("service", service);
        reqBody.put("issues", withMetadata);
        reqBody.put("pull_requests", withMetadata);
        reqBody.put("labels", withMetadata);
        reqBody.put("milestones", withMetadata);
        // 仅 GitHub 源带鉴权：用 token 走 GitHub 认证 API（5000 次/小时），避免匿名限流卡死 Issue 迁移。
        // 其它源（gitlab/gitea/裸 git）不附带，以免把 GitHub token 误发给无关主机。
        if ("github".equals(service) && authToken != null && !authToken.isBlank()) {
            reqBody.put("auth_token", authToken);
        }
        if (description != null && !description.isBlank()) {
            reqBody.put("description", description);
        }
        Map body = executeMigration(() -> client.post()
                .uri("/repos/migrate")
                .body(reqBody)
                .retrieve()
                .body(Map.class), "migrate " + cloneAddr + " -> " + owner + "/" + repoName);
        return toRepositoryInfo(body);
    }

    /**
     * 取 token 自身用户的 login，作为迁入目标 owner。结果缓存（token 在进程生命周期内不变）。
     */
    private volatile String cachedTokenOwner;

    private String resolveTokenOwner() {
        String cached = cachedTokenOwner;
        if (cached != null) {
            return cached;
        }
        Map body = execute(() -> client.get()
                .uri("/user")
                .retrieve()
                .body(Map.class), "current token user");
        String login = body == null ? null : (String) body.get("login");
        if (login == null || login.isBlank()) {
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "无法解析平台 Gitea 账号", "GET /user returned no login");
        }
        cachedTokenOwner = login;
        return login;
    }

    private RepositoryInfo getRepositoryOrNull(String owner, String repo) {
        try {
            return getRepository(owner, repo);
        } catch (BusinessException e) {
            if ("CODE_HOST_RESOURCE_NOT_FOUND".equals(e.getCode())) {
                return null;
            }
            throw e;
        }
    }

    /**
     * 按源 host 选择 Gitea 迁移 service：可识别的源平台才能带过 Issue/PR 等元数据，
     * 其余回退 {@code git}（仅克隆代码）。
     */
    private String detectService(String cloneAddr) {
        String host = RepositorySourceUrls.host(cloneAddr);
        if (host.contains("github")) return "github";
        if (host.contains("gitlab")) return "gitlab";
        if (host.contains("gitea")) return "gitea";
        return "git";
    }

    /**
     * 迁移调用的错误翻译：把地址非法 / 源不可达 / 私有需认证（422、5xx 等）统一翻成
     * 用户可读的 {@code REPOSITORY_MIGRATION_FAILED}，而非含糊的网关错误。
     */
    private <T> T executeMigration(Supplier<T> call, String resource) {
        try {
            return call.get();
        } catch (HttpClientErrorException | ResourceAccessException e) {
            throw new BusinessException("REPOSITORY_MIGRATION_FAILED", HttpStatus.UNPROCESSABLE_ENTITY,
                    "无法迁移该仓库：请确认地址正确且仓库为公开（私有仓库暂不支持）",
                    resource + " -> " + e.getMessage());
        }
    }

    @Override
    public FileCommitInfo createFile(
            String owner,
            String repo,
            String branch,
            String path,
            String message,
            String content) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("branch", branch);
        reqBody.put("message", message);
        reqBody.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        Map body = execute(() -> client.post()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .body(reqBody)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo + " create file " + path);
        return toFileCommitInfo(body, branch, path);
    }

    @Override
    public PrInfo createPullRequest(
            String owner,
            String repo,
            String title,
            String body,
            String sourceBranch,
            String targetBranch) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("title", title);
        reqBody.put("head", sourceBranch);
        reqBody.put("base", targetBranch);
        if (body != null && !body.isBlank()) {
            reqBody.put("body", body);
        }
        Map result = execute(() -> client.post()
                .uri("/repos/{owner}/{repo}/pulls", owner, repo)
                .body(reqBody)
                .retrieve()
                .body(Map.class), "repo=" + owner + "/" + repo + " create pull " + sourceBranch);
        return toPrInfo(result);
    }

    @Override
    public PrInfo mergePullRequest(String owner, String repo, int prNumber) {
        Map<String, Object> reqBody = new java.util.HashMap<>();
        reqBody.put("Do", "merge");
        reqBody.put("delete_branch_after_merge", false);
        execute(() -> client.post()
                .uri("/repos/{owner}/{repo}/pulls/{index}/merge", owner, repo, prNumber)
                .body(reqBody)
                .retrieve()
                .toBodilessEntity(), "repo=" + owner + "/" + repo + " merge pull #" + prNumber);
        return getPullRequest(owner, repo, prNumber);
    }

    /**
     * 统一执行 Gitea 调用并把传输层异常翻译为业务异常。
     *
     * <p>把 RestClient 抛出的 {@link HttpClientErrorException}（4xx）与
     * {@link ResourceAccessException}（连接失败）转成调用方可识别的 {@link BusinessException}，
     * 避免被 {@code GlobalExceptionHandler} 兜底为无信息的 HTTP 500。
     *
     * @param call     实际的 Gitea HTTP 调用
     * @param resource 资源描述，用于异常 details 定位（如 {@code "repo=owner/name pull #3"}）
     */
    private <T> T execute(Supplier<T> call, String resource) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new BusinessException("CODE_HOST_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "代码托管资源不存在", resource);
            }
            if (status == 409) {
                throw new BusinessException("CODE_HOST_RESOURCE_CONFLICT", HttpStatus.CONFLICT,
                        "代码托管资源已存在", resource);
            }
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "代码托管平台请求失败", resource + " -> HTTP " + status);
        } catch (ResourceAccessException e) {
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "代码托管平台不可达", resource);
        } catch (IllegalArgumentException e) {
            // baseUrl 未配置时 RestClient 生成无 scheme 的 URI 抛此异常；视为平台不可用
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "代码托管平台未正确配置", resource + " -> " + e.getMessage());
        }
    }

    private RepositoryInfo toRepositoryInfo(Map m) {
        return new RepositoryInfo(
                toLong(m.get("id")),
                (String) m.get("name"),
                (String) m.get("full_name"),
                (String) m.get("default_branch"),
                Boolean.TRUE.equals(m.get("empty")),
                (String) m.get("html_url"));
    }

    private IssueInfo toIssueInfo(Map m) {
        return new IssueInfo(
                toLong(m.get("number")),
                (String) m.get("title"),
                (String) m.get("body"),
                (String) m.get("state"),
                (String) m.get("html_url"));
    }

    @SuppressWarnings("unchecked")
    private PrInfo toPrInfo(Map m) {
        Map<String, Object> head = (Map<String, Object>) m.get("head");
        // Gitea head.ref is the bare branch name; head.label is "{owner}:{branch}"
        String headBranch = head != null ? (String) head.get("ref") : null;
        Map<String, Object> base = (Map<String, Object>) m.get("base");
        String baseBranch = base != null ? (String) base.get("ref") : null;
        Map<String, Object> user = (Map<String, Object>) m.get("user");
        String authorLogin = user != null ? (String) user.get("login") : null;
        // Gitea merged field is a boolean (true/false), not a nullable timestamp
        boolean merged = Boolean.TRUE.equals(m.get("merged"));
        return new PrInfo(
                toInt(m.get("number")),
                (String) m.get("title"),
                (String) m.get("state"),
                merged,
                headBranch,
                baseBranch,
                (String) m.get("html_url"),
                authorLogin);
    }

    @SuppressWarnings("unchecked")
    private FileCommitInfo toFileCommitInfo(Map m, String branch, String path) {
        Map<String, Object> commit = (Map<String, Object>) m.get("commit");
        String commitSha = commit != null ? (String) commit.get("sha") : null;
        String htmlUrl = commit != null ? (String) commit.get("html_url") : null;
        return new FileCommitInfo(commitSha, branch, path, htmlUrl);
    }

    @SuppressWarnings("unchecked")
    private BranchInfo toBranchInfo(Map m) {
        Map<String, Object> commit = (Map<String, Object>) m.get("commit");
        String sha = commit != null ? (String) commit.get("id") : null;
        return new BranchInfo((String) m.get("name"), sha);
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }
}
