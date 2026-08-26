package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 质量根因溯源历史持久化对象
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dq_root_cause_record")
public class DqRootCauseRecordPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("target_asset_id")
    private String targetAssetId;

    @TableField("root_asset_id")
    private String rootAssetId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("actual_metric")
    private String actualMetric;

    @TableField("threshold")
    private String threshold;

    @TableField("confidence")
    private String confidence;

    @TableField("fault_time")
    private String faultTime;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
