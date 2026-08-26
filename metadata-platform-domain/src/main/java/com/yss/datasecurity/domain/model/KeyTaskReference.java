package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyTaskReference {
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
