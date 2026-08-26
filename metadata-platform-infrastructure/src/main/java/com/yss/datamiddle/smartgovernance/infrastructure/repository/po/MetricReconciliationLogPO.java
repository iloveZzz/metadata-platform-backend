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
@TableName("sg_metric_reconciliation_log")
public class MetricReconciliationLogPO {
    @TableId
    private String id;
    private String conflictId;
    private String canonicalId;
    private String aliasId;
    private Integer migratedAssetCount;
    private String operator;
    private String reconcileStrategy;
    private LocalDateTime createdAt;
}
