package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 规则明细快照持久化对象（dq_rule_detail；UNIQUE(batch_id, asset_id, field_name, rule_name)）。
 *
 * <p>规则级得分快照，供钻取；weight 为计算时权重快照（MVP 固定默认，P1 配置化后读配置）；
 * 与健康分按 (batch_id, asset_id, field_name) 对齐，钻取无需回查规则结果全表（数据架构 §5/§6）。</p>
 */
@Getter
@Setter
@TableName("dq_rule_detail")
public class DqRuleDetailPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次 ID */
    @TableField("batch_id")
    private Long batchId;

    /** 资产 ID */
    @TableField("asset_id")
    private String assetId;

    /** 字段名（NULL = 资产级规则） */
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

    /** 计算时权重快照 */
    @TableField("weight")
    private Double weight;

    /** 失败原因 / 说明 */
    @TableField("failure_reason")
    private String failureReason;

    /** 工具执行时间（结果时间） */
    @TableField("execution_time")
    private LocalDateTime executionTime;

    /** 计算规则版本 */
    @TableField("rule_version")
    private String ruleVersion;
}
