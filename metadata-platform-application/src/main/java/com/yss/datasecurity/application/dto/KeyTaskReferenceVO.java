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
public class KeyTaskReferenceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long keyId;
    private String taskName;
    private String sectorName;
    private String projectName;
    private String taskType;
    private String operationType;
    private String owner;
    private LocalDateTime lastExecutedAt;
}
