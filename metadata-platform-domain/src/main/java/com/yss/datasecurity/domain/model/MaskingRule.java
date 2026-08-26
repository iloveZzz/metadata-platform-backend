package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingRule {
    private Long id;
    private String ruleName;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String algorithmType; // MASK / HASH / CRYPTO / OTHER
    private String subAlgorithm;
    private Map<String, Object> algorithmParams;
    private String applyScene; // WRITE_DEV_TABLE / DATA_QUERY / ALL
    private String maskMethod; // UNDERLYING / DISPLAY
    private String plateScope;
    private String projectScope;
    private String scopeType; // GLOBAL / DATASOURCE / PROJECT
    private Map<String, Object> scopeTarget;
    private Long keyId;
    private String keyName;
    private String owner;
    private String status; // ENABLED / DISABLED / ACTIVE
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
