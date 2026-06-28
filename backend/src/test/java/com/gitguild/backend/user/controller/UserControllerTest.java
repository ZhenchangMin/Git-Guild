package com.gitguild.backend.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import com.gitguild.backend.user.service.AuthService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthService authService = org.mockito.Mockito.mock(AuthService.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(authService, userRepository)).build();
    }

    @Test
    void publicProfileShouldExcludePrivateAccountFields() throws Exception {
        User user = new User("NovaLin", "nova@example.com", "encoded", UserRole.BEGINNER);
        user.setUserId(42L);
        user.setAvatarUrl("/uploads/avatars/nova.png");
        user.setMotto("持续交付");
        user.setDisplayBadgeId("FIRST_COMPLETION");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/users/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.username").value("NovaLin"))
                .andExpect(jsonPath("$.data.motto").value("持续交付"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }
}
