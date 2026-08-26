package com.yss.datamiddle.smartgovernance.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sg_security_audit_log")
public class SecurityAuditLogPO {
    @TableId
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
