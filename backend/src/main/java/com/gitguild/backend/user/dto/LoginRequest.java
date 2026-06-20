package com.gitguild.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    /** 登录标识：可填用户名或邮箱。不再强制邮箱格式，以支持用户名登录。 */
    @NotBlank(message = "account 不能为空")
    private String account;

    @NotBlank(message = "password 不能为空")
    private String password;

    private boolean remember;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }
}
