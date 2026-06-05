package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.common.BusinessException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

/**
 * 从 {@code CodeRepository.sourceUrl} 解析出 Gitea 的 owner/repo 坐标。
 *
 * <p>支持三种常见仓库地址形态：
 * <ul>
 *   <li>HTTP(S)：{@code http(s)://host/owner/repo(.git)}</li>
 *   <li>scp 风格：{@code git@host:owner/repo(.git)}</li>
 *   <li>裸路径：{@code owner/repo}</li>
 * </ul>
 *
 * <p>抽取自 {@code CodePullRequestSyncServiceImpl} / {@code CodeIssueServiceImpl}，
 * 供所有需要"按仓库地址定位 Gitea 仓库"的代理写操作复用，避免解析逻辑多处漂移。
 *
 * @param owner Gitea 仓库 owner
 * @param repo  Gitea 仓库名
 */
public record GiteaRepoCoordinates(String owner, String repo) {

    /**
     * 解析仓库地址。解析不出 owner/repo 时抛 {@code REPOSITORY_SOURCE_URL_INVALID}。
     */
    public static GiteaRepoCoordinates parse(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw invalidSourceUrl(sourceUrl);
        }
        String s = stripQueryAndFragment(sourceUrl.trim());
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".git")) {
            s = s.substring(0, s.length() - 4);
        }

        String path;
        int proto = s.indexOf("://");
        if (proto >= 0) {
            String afterProto = s.substring(proto + 3);
            int slash = afterProto.indexOf('/');
            path = slash >= 0 ? afterProto.substring(slash + 1) : "";
        } else if (s.contains("@") && s.contains(":")) {
            path = s.substring(s.indexOf(':') + 1);
        } else {
            path = s;
        }

        String[] parts = Arrays.stream(path.split("/"))
                .filter(p -> !p.isBlank())
                .toArray(String[]::new);
        if (parts.length < 2) {
            throw invalidSourceUrl(sourceUrl);
        }
        return new GiteaRepoCoordinates(parts[parts.length - 2], parts[parts.length - 1]);
    }

    private static String stripQueryAndFragment(String s) {
        int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        return s;
    }

    private static BusinessException invalidSourceUrl(String sourceUrl) {
        return new BusinessException("REPOSITORY_SOURCE_URL_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                "无法从仓库地址解析出 owner/repo", "sourceUrl=" + sourceUrl);
    }
}
