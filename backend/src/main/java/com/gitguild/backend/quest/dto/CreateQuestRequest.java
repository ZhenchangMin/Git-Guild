package com.gitguild.backend.quest.dto;

import com.gitguild.backend.quest.domain.Difficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateQuestRequest {

    @NotNull(message = "repositoryId 不能为空")
    private Long repositoryId;

    /**
     * 关联已有本地 Issue（与 {@code giteaIssueTitle} 二选一）。
     * 如果同时提供 {@code giteaIssueTitle}，则忽略此字段，走 Gitea 创建路径。
     */
    private Long issueId;

    /**
     * 在 Gitea 上创建新 Issue 的标题（与 {@code issueId} 二选一）。
     * 提供此字段时，后端会在 Gitea 仓库中创建 Issue 并同步到本地绑定。
     */
    @Size(max = 200, message = "giteaIssueTitle 长度不能超过 200")
    private String giteaIssueTitle;

    /**
     * 在 Gitea 上创建新 Issue 的正文（可选）。
     */
    private String giteaIssueBody;

    @NotBlank(message = "title 不能为空")
    @Size(max = 200, message = "title 长度不能超过 200")
    private String title;

    @NotBlank(message = "description 不能为空")
    private String description;

    @NotBlank(message = "completionCriteria 不能为空")
    private String completionCriteria;

    @NotNull(message = "difficulty 不能为空")
    private Difficulty difficulty;

    @NotEmpty(message = "techStack 不能为空")
    private List<String> techStack;

    @NotNull(message = "estimatedHours 不能为空")
    @Min(value = 1, message = "estimatedHours 必须大于 0")
    private Integer estimatedHours;

    @NotNull(message = "rewardXp 不能为空")
    @Min(value = 1, message = "rewardXp 必须大于 0")
    private Integer rewardXp;

    @NotNull(message = "categoryId 不能为空")
    private Long categoryId;

    private List<Long> tagIds = List.of();

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public String getGiteaIssueTitle() {
        return giteaIssueTitle;
    }

    public void setGiteaIssueTitle(String giteaIssueTitle) {
        this.giteaIssueTitle = giteaIssueTitle;
    }

    public String getGiteaIssueBody() {
        return giteaIssueBody;
    }

    public void setGiteaIssueBody(String giteaIssueBody) {
        this.giteaIssueBody = giteaIssueBody;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompletionCriteria() {
        return completionCriteria;
    }

    public void setCompletionCriteria(String completionCriteria) {
        this.completionCriteria = completionCriteria;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTechStack() {
        return techStack;
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Integer getRewardXp() {
        return rewardXp;
    }

    public void setRewardXp(Integer rewardXp) {
        this.rewardXp = rewardXp;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds == null ? List.of() : tagIds;
    }
}
