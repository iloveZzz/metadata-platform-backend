package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummaryDTO implements Serializable {
    private String batchId;
    private Integer totalProcessed;
    private Integer autoAppliedCount;
    private Integer pendingReviewCount;
}
