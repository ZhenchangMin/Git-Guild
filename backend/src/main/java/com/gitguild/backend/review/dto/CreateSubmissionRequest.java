package com.gitguild.backend.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateSubmissionRequest {

    @NotNull(message = "questId cannot be null")
    private Long questId;

    /**
     * 已废弃：平台现在依据 task 分支自动创建/复用 PR，提交时无需再传。
     * 字段保留为可选以兼容旧客户端，服务端忽略其值。
     */
    @Deprecated
    private Long pullRequestId;

    @NotBlank(message = "description cannot be blank")
    private String description;

    private List<@NotBlank(message = "checklist item cannot be blank") String> checklist = List.of();

    private List<@Size(max = 512, message = "evidenceUrl length cannot exceed 512") String> evidenceUrls = List.of();

    public Long getQuestId() {
        return questId;
    }

    public void setQuestId(Long questId) {
        this.questId = questId;
    }

    public Long getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(Long pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<String> checklist) {
        this.checklist = checklist == null ? List.of() : checklist;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls == null ? List.of() : evidenceUrls;
    }
}
