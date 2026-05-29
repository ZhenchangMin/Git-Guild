package com.gitguild.backend.review.dto;

import com.gitguild.backend.review.domain.ReviewDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ReviewSubmissionRequest {

    @NotNull(message = "decision cannot be null")
    private ReviewDecision decision;

    @NotBlank(message = "summary cannot be blank")
    @Size(max = 500, message = "summary length cannot exceed 500")
    private String summary;

    @Valid
    private List<ReviewItemRequest> items = List.of();

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ReviewItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReviewItemRequest> items) {
        this.items = items == null ? List.of() : items;
    }

    public static class ReviewItemRequest {

        @NotBlank(message = "checkpoint cannot be blank")
        @Size(max = 128, message = "checkpoint length cannot exceed 128")
        private String checkpoint;

        @Size(max = 500, message = "comment length cannot exceed 500")
        private String comment;

        @NotNull(message = "passed cannot be null")
        private Boolean passed;

        public String getCheckpoint() {
            return checkpoint;
        }

        public void setCheckpoint(String checkpoint) {
            this.checkpoint = checkpoint;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Boolean getPassed() {
            return passed;
        }

        public void setPassed(Boolean passed) {
            this.passed = passed;
        }
    }
}
