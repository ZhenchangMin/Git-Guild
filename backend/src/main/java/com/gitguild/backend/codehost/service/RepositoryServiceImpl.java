package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.dto.CreateRepositoryRequest;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaProperties;
import com.gitguild.backend.codehost.gitea.RepositorySourceUrls;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryServiceImpl implements RepositoryService {

    private final GiteaAdapter giteaAdapter;
    private final GiteaProperties giteaProperties;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final UserRepository userRepository;
    /** 迁移外部 GitHub 仓库 Issue 时使用的鉴权 token（env GITHUB_TOKEN 注入，未配置则为空）。 */
    private final String migrationToken;

    public RepositoryServiceImpl(
            GiteaAdapter giteaAdapter,
            GiteaProperties giteaProperties,
            CodeRepositoryRepository codeRepositoryRepository,
            UserRepository userRepository,
            @Value("${gitea.migration-token:}") String migrationToken) {
        this.giteaAdapter = giteaAdapter;
        this.giteaProperties = giteaProperties;
        this.codeRepositoryRepository = codeRepositoryRepository;
        this.userRepository = userRepository;
        this.migrationToken = migrationToken;
    }

    @Override
    @Transactional
    public CodeRepository createRepository(Long currentUserId, CreateRepositoryRequest request) {
        User owner = findUser(currentUserId);
        if (owner.getRole() != UserRole.MAINTAINER && owner.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN,
                    "仅 Guild Master 或 Admin 可创建仓库",
                    "userId=" + currentUserId + " role=" + owner.getRole());
        }

        // 在 admin 用户下创建 Gitea 仓库（决策 1 甲）
        RepositoryInfo info = giteaAdapter.createRepository(request.getName(), request.getDescription());

        CodeRepository repository = new CodeRepository(owner, info.name(), "GITEA", info.htmlUrl());
        repository.setDefaultBranch(info.defaultBranch());
        repository.setExternalRepositoryId(String.valueOf(info.id()));
        if (request.getDescription() != null) {
            repository.setDescription(request.getDescription());
        }
        return codeRepositoryRepository.save(repository);
    }

    private static final String DEFAULT_HOST_TYPE = "GITEA";

    @Override
    @Transactional
    public CodeRepository importRepository(Long currentUserId, String sourceUrl, String name, String hostType) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "请求参数不合法", "sourceUrl is required");
        }
        User owner = findUser(currentUserId);
        String trimmedSource = sourceUrl.trim();
        String resolvedHostType = hostType == null || hostType.isBlank()
                ? DEFAULT_HOST_TYPE
                : hostType.trim();

        // 外部源：自动迁入平台 Gitea，再以平台副本地址登记
        if (RepositorySourceUrls.isExternalSource(trimmedSource, giteaProperties.baseUrl())) {
            String targetName = RepositorySourceUrls.deterministicRepoName(trimmedSource);
            RepositoryInfo info = giteaAdapter.migrateRepository(trimmedSource, targetName, name, true, migrationToken);
            return findOrCreate(resolvedHostType, info.htmlUrl(), owner, info.name(), info);
        }

        // 源已在平台自己的 Gitea 上（或平台地址未知）：直接登记，无需迁移（幂等）
        String displayName = name == null || name.isBlank() ? inferName(trimmedSource) : name.trim();
        return findOrCreate(resolvedHostType, trimmedSource, owner, displayName, null);
    }

    private CodeRepository findOrCreate(
            String hostType, String sourceUrl, User owner, String name, RepositoryInfo info) {
        return codeRepositoryRepository
                .findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc(hostType, sourceUrl)
                .orElseGet(() -> {
                    CodeRepository repository = new CodeRepository(owner, name, hostType, sourceUrl);
                    if (info != null) {
                        repository.setDefaultBranch(info.defaultBranch());
                        repository.setExternalRepositoryId(String.valueOf(info.id()));
                    }
                    return codeRepositoryRepository.save(repository);
                });
    }

    private String inferName(String sourceUrl) {
        try {
            String path = URI.create(sourceUrl).getPath();
            String name = path == null || path.isBlank()
                    ? "repository"
                    : path.substring(path.lastIndexOf('/') + 1);
            return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
        } catch (IllegalArgumentException ex) {
            return "repository";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodeRepository> listRepositories(Long currentUserId) {
        User user = findUser(currentUserId);
        // 仓库是平台共享资源：Guild Master / Admin 发布委托时需要看到平台上所有已接入仓库，
        // 而不仅是自己点"导入"的那批——否则用后端/他人身份导入的仓库在发布页选不到（owner 不匹配）。
        // 其余角色仅能看自己拥有的仓库。
        if (user.getRole() == UserRole.MAINTAINER || user.getRole() == UserRole.ADMIN) {
            return codeRepositoryRepository.findAll(Sort.by(Sort.Direction.ASC, "repositoryId"));
        }
        return codeRepositoryRepository.findByOwnerUserId(currentUserId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "User not found", "userId=" + userId));
    }
}
