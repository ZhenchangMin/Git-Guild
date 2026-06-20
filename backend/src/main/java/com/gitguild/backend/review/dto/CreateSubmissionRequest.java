package com.gitguild.backend.review.dto;

import jakarta.validation.Valid;
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

    /**
     * 冒险家上传的佐证文件（内联 base64 data URL）。委托人将在审核台查看这些文件。
     * 用单独字段而非复用 evidenceUrls（后者每项 512 字符上限无法承载文件内容）。
     */
    @Size(max = 5, message = "evidenceFiles cannot exceed 5 items")
    private List<@Valid EvidenceFile> evidenceFiles = List.of();

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

    public List<EvidenceFile> getEvidenceFiles() {
        return evidenceFiles;
    }

    public void setEvidenceFiles(List<EvidenceFile> evidenceFiles) {
        this.evidenceFiles = evidenceFiles == null ? List.of() : evidenceFiles;
    }

    /** 单个佐证文件：文件名 + MIME 类型 + 内联 base64 data URL 内容。 */
    public static class EvidenceFile {

        @NotBlank(message = "evidence file name cannot be blank")
        @Size(max = 255, message = "evidence file name cannot exceed 255")
        private String name;

        @Size(max = 128, message = "evidence file mimeType cannot exceed 128")
        private String mimeType;

        // ~7MB 的 data URL 上限（对应 5MB 原文件 base64 后约 6.7MB），防止异常巨大的请求体。
        @NotBlank(message = "evidence file content cannot be blank")
        @Size(max = 7_500_000, message = "evidence file content too large")
        private String content;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
