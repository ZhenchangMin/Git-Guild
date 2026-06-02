package com.gitguild.backend.codehost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建本地 Gitea 仓库的请求体（Issue #9 POST /api/v1/repositories）。
 */
public class CreateRepositoryRequest {

    @NotBlank(message = "仓库名不能为空")
    @Size(max = 128, message = "仓库名长度不能超过 128")
    private String name;

    @Size(max = 512, message = "仓库描述长度不能超过 512")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
