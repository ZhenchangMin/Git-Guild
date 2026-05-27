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

    @NotNull(message = "issueId 不能为空")
    private Long issueId;

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
