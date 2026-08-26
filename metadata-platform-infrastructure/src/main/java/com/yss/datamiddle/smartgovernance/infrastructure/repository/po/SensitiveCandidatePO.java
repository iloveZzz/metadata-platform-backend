package com.yss.datamiddle.smartgovernance.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sg_candidate")
public class SensitiveCandidatePO {
    @TableId
    private String id;
    private String templateId;
    private String ruleId;
    private String dataSource;
    private String databaseName;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String sensitiveType;
    private String recommendedLevel;
    private String clauseRef;
    private String reasoning;
    private BigDecimal confidence;
    private String funnelLayer;
    private String status;
    private String actualLevel;
    private String operator;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
