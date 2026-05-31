package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.common.BusinessException;
import java.util.Arrays;
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
 * <p>使用系统级 admin token 认证，与具体 Adventurer 身份无关。
 * token 通过 {@link GiteaProperties} 从环境变量注入；token 为空时应用可启动，
 * 但所有调用将收到 401（见已知问题清单 P4-016 问题2）。
 *
 * <p><b>边界错误模式：</b>Gitea 返回 4xx 或网络不可达时，不再透传为 HTTP 500，
 * 而是经 {@link #execute} 统一转换为带业务码的 {@link BusinessException}
 * （{@code CODE_HOST_RESOURCE_NOT_FOUND} / {@code CODE_HOST_UNAVAILABLE}），
 * 兑现已知问题清单 P4-016 问题1 的修复承诺。
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
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "代码托管平台请求失败", resource + " -> HTTP " + status);
        } catch (ResourceAccessException e) {
            throw new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                    "代码托管平台不可达", resource);
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
