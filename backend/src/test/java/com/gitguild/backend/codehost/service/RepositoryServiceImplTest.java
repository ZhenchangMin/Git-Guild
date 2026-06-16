package com.gitguild.backend.codehost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceImplTest {

    @Mock private GiteaAdapter giteaAdapter;
    @Mock private CodeRepositoryRepository codeRepositoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private RepositoryCascadeDeleter cascadeDeleter;
    private final GiteaProperties giteaProperties = new GiteaProperties("http://localhost:3000", "", "spike-admin", null);

    private static final String MIGRATION_TOKEN = "gh-migration-token";

    private RepositoryServiceImpl service() {
        return new RepositoryServiceImpl(giteaAdapter, giteaProperties,
                codeRepositoryRepository, userRepository, cascadeDeleter, MIGRATION_TOKEN);
    }

    @Test
    void createRepositoryByMaintainer() {
        User u = mockUser(2001L, UserRole.MAINTAINER);
        CreateRepositoryRequest req = new CreateRepositoryRequest();
        req.setName("demo-repo");
        req.setDescription("a test repo");

        RepositoryInfo info = new RepositoryInfo(3L, "demo-repo",
                "spike-admin/demo-repo", "main", true,
                "http://localhost:3000/spike-admin/demo-repo");

        when(userRepository.findById(2001L)).thenReturn(Optional.of(u));
        when(giteaAdapter.createRepository("demo-repo", "a test repo")).thenReturn(info);
        when(codeRepositoryRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        CodeRepository result = service().createRepository(2001L, req);

        assertThat(result.getName()).isEqualTo("demo-repo");
        assertThat(result.getHostType()).isEqualTo("GITEA");
        assertThat(result.getSourceUrl()).isEqualTo("http://localhost:3000/spike-admin/demo-repo");
        assertThat(result.getDefaultBranch()).isEqualTo("main");
        assertThat(result.getDescription()).isEqualTo("a test repo");
    }

    @Test
    void rejectsNonMaintainerRole() {
        User u = mockUser(3001L, UserRole.BEGINNER);
        CreateRepositoryRequest req = new CreateRepositoryRequest();
        req.setName("demo-repo");

        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service().createRepository(3001L, req))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void importExternalRepositoryMigratesIntoPlatformGitea() {
        User u = mockUser(42L, UserRole.MAINTAINER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(u));

        String externalUrl = "https://gitea.com/ZhenchangMin/Operating-System.git";
        String platformUrl = "http://localhost:3000/spike-admin/ZhenchangMin-Operating-System";
        RepositoryInfo info = new RepositoryInfo(9L, "ZhenchangMin-Operating-System",
                "spike-admin/ZhenchangMin-Operating-System", "main", false, platformUrl);

        when(giteaAdapter.migrateRepository(eq(externalUrl),
                eq("ZhenchangMin-Operating-System"), eq("OS 课程仓库"), eq(true), eq(MIGRATION_TOKEN)))
                .thenReturn(info);
        when(codeRepositoryRepository
                .findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc("GITEA", platformUrl))
                .thenReturn(Optional.empty());
        when(codeRepositoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeRepository result = service().importRepository(42L, externalUrl, "OS 课程仓库", "GITEA");

        // 外部源应被迁入平台，登记的是平台副本地址而非原始外网地址
        assertThat(result.getSourceUrl()).isEqualTo(platformUrl);
        assertThat(result.getName()).isEqualTo("ZhenchangMin-Operating-System");
        assertThat(result.getDefaultBranch()).isEqualTo("main");
        assertThat(result.getExternalRepositoryId()).isEqualTo("9");
        verify(giteaAdapter).migrateRepository(eq(externalUrl),
                eq("ZhenchangMin-Operating-System"), eq("OS 课程仓库"), eq(true), eq(MIGRATION_TOKEN));
    }

    @Test
    void importInternalRepositoryRegistersWithoutMigration() {
        User u = mockUser(42L, UserRole.MAINTAINER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(u));

        String internalUrl = "http://localhost:3000/spike-admin/demo-repo.git";
        when(codeRepositoryRepository
                .findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc("GITEA", internalUrl))
                .thenReturn(Optional.empty());
        when(codeRepositoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeRepository result = service().importRepository(42L, internalUrl, null, "GITEA");

        // 源就在平台自己的 Gitea 上：直接登记，名称由地址推断，不触发迁移
        assertThat(result.getSourceUrl()).isEqualTo(internalUrl);
        assertThat(result.getName()).isEqualTo("demo-repo");
        verify(giteaAdapter, never()).migrateRepository(anyString(), anyString(), any(), anyBoolean(), any());
    }

    @Test
    void importRejectsBlankSourceUrl() {
        assertThatThrownBy(() -> service().importRepository(42L, "  ", null, "GITEA"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void listReturnsOwnedRepositoriesForNonPrivilegedRole() {
        User u = mockUser(2001L, UserRole.BEGINNER);
        when(userRepository.findById(2001L)).thenReturn(Optional.of(u));
        when(codeRepositoryRepository.findByOwnerUserId(2001L)).thenReturn(List.of());

        assertThat(service().listRepositories(2001L)).isEmpty();
        verify(codeRepositoryRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void listReturnsAllRepositoriesForMaintainer() {
        User u = mockUser(7001L, UserRole.MAINTAINER);
        when(userRepository.findById(7001L)).thenReturn(Optional.of(u));
        when(codeRepositoryRepository.findAll(any(Sort.class))).thenReturn(List.of());

        // 维护者发布委托时应看到平台全部仓库，而非仅自己导入的
        assertThat(service().listRepositories(7001L)).isEmpty();
        verify(codeRepositoryRepository).findAll(any(Sort.class));
        verify(codeRepositoryRepository, never()).findByOwnerUserId(anyLong());
    }

    @Test
    void deleteRepositoryByMaintainerInvokesCascade() {
        User u = mockUser(2001L, UserRole.MAINTAINER);
        CodeRepository repo = new CodeRepository(u, "demo-repo", "GITEA",
                "http://localhost:3000/spike-admin/demo-repo");
        when(userRepository.findById(2001L)).thenReturn(Optional.of(u));
        when(codeRepositoryRepository.findById(55L)).thenReturn(Optional.of(repo));

        service().deleteRepository(2001L, 55L);

        verify(cascadeDeleter).deleteCascade(repo);
    }

    @Test
    void deleteRepositoryRejectsNonPrivilegedRole() {
        User u = mockUser(3001L, UserRole.BEGINNER);
        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service().deleteRepository(3001L, 55L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
        verify(cascadeDeleter, never()).deleteCascade(any());
    }

    @Test
    void deleteRepositoryNotFound() {
        User u = mockUser(2001L, UserRole.MAINTAINER);
        when(userRepository.findById(2001L)).thenReturn(Optional.of(u));
        when(codeRepositoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteRepository(2001L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("REPOSITORY_NOT_FOUND");
        verify(cascadeDeleter, never()).deleteCascade(any());
    }

    private User mockUser(Long id, UserRole role) {
        User u = org.mockito.Mockito.mock(User.class);
        lenient().when(u.getUserId()).thenReturn(id);
        lenient().when(u.getRole()).thenReturn(role);
        return u;
    }
}
