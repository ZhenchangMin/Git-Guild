package com.gitguild.backend.user.dto;

import com.gitguild.backend.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "username 不能为空")
    @Size(min = 1, max = 32, message = "username 长度必须为 1-32")
    private String username;

    @NotBlank(message = "email 不能为空")
    @Email(message = "email 格式不正确")
    @Size(max = 128, message = "email 长度不能超过 128")
    private String email;

    @NotBlank(message = "password 不能为空")
    @Size(min = 8, max = 64, message = "password 长度必须为 8-64")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password 必须同时包含字母和数字")
    private String password;

    private UserRole role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
