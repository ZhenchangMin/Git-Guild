package com.gitguild.backend.quest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 审核检查清单单项：通过上架时随附的提示性检查项，供管理员审阅时重点关注，不需要逐项操作。
 *
 * <p>与历史落库的 {@code AdminReviewRecord.checklistJson} 一一对应，前端的「清晰度检查」「合规性检查」
 * 共 6 项最终都序列化为这个结构，使审核结论可追溯到展示过哪些关注点。
 */
public class ChecklistItemDto {

    @NotBlank(message = "检查项名称不能为空")
    private String label;

    private boolean passed;

    public ChecklistItemDto() {
    }

    public ChecklistItemDto(String label, boolean passed) {
        this.label = label;
        this.passed = passed;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
