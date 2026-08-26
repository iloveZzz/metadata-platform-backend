package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionBatchLogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String batchType; // IMPORT / MANUAL_ADD / BATCH_EDIT
    private String fileName;
    private String assetType;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String conflictStrategy;
    private String maskingPolicy;
    private String status; // SUCCESS / PARTIAL_FAILED / FAILED
    private String errorReportUrl;
    private String operator;
    private LocalDateTime createdAt;
}
