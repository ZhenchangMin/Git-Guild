package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        Map body = client.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(Map.class);
        return toRepositoryInfo(body);
    }

    @Override
    public List<IssueInfo> listIssues(String owner, String repo) {
        Map[] body = client.get()
                .uri("/repos/{owner}/{repo}/issues?type=issues&state=open&limit=50", owner, repo)
                .retrieve()
                .body(Map[].class);
        if (body == null) return List.of();
        return Arrays.stream(body).map(this::toIssueInfo).toList();
    }

    @Override
    public PrInfo getPullRequest(String owner, String repo, int prNumber) {
        Map body = client.get()
                .uri("/repos/{owner}/{repo}/pulls/{index}", owner, repo, prNumber)
                .retrieve()
                .body(Map.class);
        return toPrInfo(body);
    }

    @Override
    public List<BranchInfo> listBranches(String owner, String repo) {
        Map[] body = client.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .retrieve()
                .body(Map[].class);
        if (body == null) return List.of();
        return Arrays.stream(body).map(this::toBranchInfo).toList();
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
