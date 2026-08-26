package com.yss.datamiddle.smartgovernance.domain.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全打标全链路审计留痕 (Entity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String candidateId;
    private String dataSource;
    private String databaseName;
    private String tableName;
    private String columnName;
    private String previousLevel;
    private String newLevel;
    private String actionType;
    private String operator;
    private String reason;
    private LocalDateTime createdAt;
}
