package com.gitguild.backend.user.dto;

import jakarta.validation.constraints.Size;

/**
 * PATCH /api/v1/users/me — 更新用户可编辑资料（座右铭等）。
 * 所有字段可选，传 null 表示不修改。
 */
public class UpdateProfileRequest {

    @Size(max = 200, message = "座右铭不能超过 200 个字符")
    private String motto;

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }
}
