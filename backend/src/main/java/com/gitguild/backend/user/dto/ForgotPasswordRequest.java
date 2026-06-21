package com.gitguild.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 忘记密码第一步：请求向指定邮箱发送重置验证码。 */
public class ForgotPasswordRequest {

    @NotBlank(message = "email 不能为空")
    @Email(message = "email 格式不正确")
    @Size(max = 128, message = "email 长度不能超过 128")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
