package com.gitguild.backend.codehost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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

@ExtendWith(MockitoExtension.class)
class RepositoryServiceImplTest {

    @Mock private GiteaAdapter giteaAdapter;
    @Mock private CodeRepositoryRepository codeRepositoryRepository;
    @Mock private UserRepository userRepository;
    private final GiteaProperties giteaProperties = new GiteaProperties("http://localhost:3000", "", "spike-admin");

    private RepositoryServiceImpl service() {
        return new RepositoryServiceImpl(giteaAdapter, giteaProperties,
                codeRepositoryRepository, userRepository);
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
    void listReturnsOwnerRepositories() {
        when(codeRepositoryRepository.findByOwnerUserId(2001L)).thenReturn(List.of());

        assertThat(service().listRepositories(2001L)).isEmpty();
    }

    private User mockUser(Long id, UserRole role) {
        User u = org.mockito.Mockito.mock(User.class);
        lenient().when(u.getUserId()).thenReturn(id);
        lenient().when(u.getRole()).thenReturn(role);
        return u;
    }
}
