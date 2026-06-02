package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.dto.CreateRepositoryRequest;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaProperties;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryServiceImpl implements RepositoryService {

    private final GiteaAdapter giteaAdapter;
    private final GiteaProperties giteaProperties;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final UserRepository userRepository;

    public RepositoryServiceImpl(
            GiteaAdapter giteaAdapter,
            GiteaProperties giteaProperties,
            CodeRepositoryRepository codeRepositoryRepository,
            UserRepository userRepository) {
        this.giteaAdapter = giteaAdapter;
        this.giteaProperties = giteaProperties;
        this.codeRepositoryRepository = codeRepositoryRepository;
        this.userRepository = userRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<CodeRepository> listRepositories(Long currentUserId) {
        // Guild Master / Admin 可查看自己拥有的仓库
        return codeRepositoryRepository.findByOwnerUserId(currentUserId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "User not found", "userId=" + userId));
    }
}
