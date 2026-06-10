package com.gitguild.backend.quest.dto;

import com.gitguild.backend.quest.domain.AdminDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminReviewRequest {

    @NotNull(message = "审核决策不能为空")
    private AdminDecision decision;

    @NotBlank(message = "审核原因不能为空")
    @Size(max = 500, message = "审核原因不超过 500 字")
    private String reason;

    private boolean visibleToPublisher = true;

    public AdminDecision getDecision() { return decision; }
    public void setDecision(AdminDecision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isVisibleToPublisher() { return visibleToPublisher; }
    public void setVisibleToPublisher(boolean visibleToPublisher) { this.visibleToPublisher = visibleToPublisher; }
}
