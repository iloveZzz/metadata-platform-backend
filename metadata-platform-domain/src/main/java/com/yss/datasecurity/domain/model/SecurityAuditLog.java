package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 全链路安全审计日志领域模型实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLog {
    private Long id;
    private String actionType; // SENSITIVE_SCAN, MANUAL_LOCK, KEY_REVEAL, WHITELIST_GRANT, WHITELIST_REVOKE, MASK_QUERY
    private String operator;
    private String clientIp;
    private String targetResource;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private String actionDetail;
    private LocalDateTime createdAt;
}
