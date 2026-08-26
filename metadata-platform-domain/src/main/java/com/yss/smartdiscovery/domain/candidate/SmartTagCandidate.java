package com.yss.smartdiscovery.domain.candidate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartTagCandidate {
    private String id;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String currentTag;
    private String recommendedTagId;
    private String recommendedTagName;
    private String tagCategory;
    private String source; // L1_RULE, L2_DICT, L3_LLM
    private Double confidence;
    private String inferenceReason;
    private String status; // PENDING, AUTO_APPLIED, MANUAL_APPROVED, REJECTED, ROLLED_BACK
    private String batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void applyAutomatically(double threshold) {
        if (this.confidence != null && this.confidence >= threshold) {
            this.status = "AUTO_APPLIED";
        } else {
            this.status = "PENDING";
        }
    }

    public void approveManually() {
        this.status = "MANUAL_APPROVED";
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = "REJECTED";
        this.inferenceReason = (this.inferenceReason != null ? this.inferenceReason + " | " : "") + "驳回原因: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void rollback() {
        this.status = "ROLLED_BACK";
        this.updatedAt = LocalDateTime.now();
    }
}
