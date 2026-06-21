package com.gitguild.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 忘记密码第二步：携带邮箱、验证码与新密码完成重置。 */
public class ResetPasswordRequest {

    @NotBlank(message = "email 不能为空")
    @Email(message = "email 格式不正确")
    @Size(max = 128, message = "email 长度不能超过 128")
    private String email;

    @NotBlank(message = "code 不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为 6 位数字")
    private String code;

    @NotBlank(message = "newPassword 不能为空")
    @Size(min = 8, max = 64, message = "newPassword 长度必须为 8-64")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "newPassword 必须同时包含字母和数字")
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
