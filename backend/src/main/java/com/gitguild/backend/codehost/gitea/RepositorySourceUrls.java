package com.gitguild.backend.codehost.gitea;

import java.util.Locale;

/**
 * 仓库源地址的纯函数工具：host 提取、内/外网同源判定、平台内确定性命名。
 *
 * <p>从「自动迁移外部仓库进平台 Gitea」流程中抽出，与 {@link GiteaRepoCoordinates}（owner/repo
 * 坐标解析）职责互补——这里关心的是 host 维度与迁入后的目标命名。所有方法无状态、可独立单测。
 */
public final class RepositorySourceUrls {

    private RepositorySourceUrls() {
    }

    /**
     * 取仓库地址的 host（小写、去端口）。支持 http(s):// 与 scp 风格 {@code git@host:owner/repo}。
     * 解析不出时返回空串。
     */
    public static String host(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String s = url.trim();
        int proto = s.indexOf("://");
        if (proto >= 0) {
            String afterProto = s.substring(proto + 3);
            int slash = afterProto.indexOf('/');
            String authority = slash >= 0 ? afterProto.substring(0, slash) : afterProto;
            return stripUserInfoAndPort(authority);
        }
        // scp 风格：git@host:owner/repo
        if (s.contains("@") && s.contains(":")) {
            String afterAt = s.substring(s.indexOf('@') + 1);
            int colon = afterAt.indexOf(':');
            String hostPart = colon >= 0 ? afterAt.substring(0, colon) : afterAt;
            return hostPart.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    /**
     * 源仓库是否为「外部源」——需自动迁入平台 Gitea。
     *
     * <p><b>保守判定：</b>仅当源 host 与平台 host <b>都能解析出且不相同</b>时才视为外部。
     * 若平台 baseUrl 未配置（host 解析为空，如某些测试 profile）或两者同 host，则一律按内部处理，
     * 绝不在「平台地址未知」时贸然触发迁移（迁移目标无从校验，且会击穿 mock/真实 Gitea）。
     *
     * @param sourceUrl       源仓库地址
     * @param platformBaseUrl 平台 Gitea 的 base URL（{@code gitea.base-url}）
     */
    public static boolean isExternalSource(String sourceUrl, String platformBaseUrl) {
        String src = host(sourceUrl);
        String platform = host(platformBaseUrl);
        return !src.isBlank() && !platform.isBlank() && !src.equals(platform);
    }

    /**
     * 由源地址派生平台内确定性仓库名：{@code owner-repo}，并 sanitize 成 Gitea 合法名。
     *
     * <p>含 owner 以避免不同 owner 的同名仓库在平台内相撞；确定性保证「同一外网地址重复导入」
     * 收敛到同一平台副本，从而幂等。
     */
    public static String deterministicRepoName(String url) {
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(url);
        return sanitize(coords.owner() + "-" + coords.repo());
    }

    private static String stripUserInfoAndPort(String authority) {
        String a = authority;
        int at = a.indexOf('@');
        if (at >= 0) {
            a = a.substring(at + 1);
        }
        int colon = a.indexOf(':');
        if (colon >= 0) {
            a = a.substring(0, colon);
        }
        return a.toLowerCase(Locale.ROOT);
    }

    /** Gitea 仓库名允许字母数字、{@code . _ -}；其余字符折叠为 {@code -}。 */
    private static String sanitize(String raw) {
        String cleaned = raw.replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-{2,}", "-");
        cleaned = cleaned.replaceAll("^-+", "").replaceAll("-+$", "");
        return cleaned.isBlank() ? "repository" : cleaned;
    }
}
