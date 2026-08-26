package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 规则结果持久化对象（dq_rule_result；单批次 ≤5 万行，批量 INSERT，解析成功即入库）。
 */
@Getter
@Setter
@TableName("dq_rule_result")
public class DqRuleResultPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次 ID */
    @TableField("batch_id")
    private Long batchId;

    /** 资产 ID（可空；空 = 资产级规则） */
    @TableField("asset_id")
    private String assetId;

    /** 字段名（空 = 资产级规则） */
    @TableField("field_name")
    private String fieldName;

    /** 规则名 */
    @TableField("rule_name")
    private String ruleName;

    /** 规则类型 */
    @TableField("rule_type")
    private String ruleType;

    /** 规则结果状态 */
    @TableField("status")
    private String status;

    /** 失败原因 / 说明 */
    @TableField("failure_reason")
    private String failureReason;

    /** 工具执行时间 */
    @TableField("execution_time")
    private LocalDateTime executionTime;
}
