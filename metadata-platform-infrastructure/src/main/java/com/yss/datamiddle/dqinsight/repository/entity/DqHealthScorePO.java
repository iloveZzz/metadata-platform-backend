package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 健康分持久化对象（dq_health_score；UNIQUE(asset_id, field_name) 每资产 / 字段保留最新）。
 *
 * <p>资产级 field_name NULL 与字段级共用一表（数据架构 §5）；资产快照（名称 / 域 / 类型）来自防腐层
 * 冗余用于域过滤与展示免 join（数据架构 §10）；last_result_at 为冻结 OpenAPI lastResultAt 字段物理落点
 * （工具执行时间快照，随计算写入）。</p>
 */
@Getter
@Setter
@TableName("dq_health_score")
public class DqHealthScorePO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 资产 ID */
    @TableField("asset_id")
    private String assetId;

    /** 字段名（NULL = 资产级健康分） */
    @TableField("field_name")
    private String fieldName;

    /** 资产名称快照 */
    @TableField("asset_name")
    private String assetName;

    /** 数据域快照 */
    @TableField("domain")
    private String domain;

    /** 资产类型快照 */
    @TableField("asset_type")
    private String assetType;

    /** 健康分 0~100 */
    @TableField("score")
    private Integer score;

    /**
     * 档位（优 / 良 / 差）。
     *
     * <p>列名 band（数据架构 §5）；Java 字段名 healthBand 规避 MyBatis OGNL 关键字
     * （OGNL 3.2.6 保留 band 为位与操作符 token，实体字段作为 <if test> 属性名会解析失败）。</p>
     */
    @TableField("band")
    private String healthBand;

    /** 状态（ok / expired / noresult / calculating；过期由查询派生） */
    @TableField("state")
    private String state;

    /** 计算规则版本（如 v3） */
    @TableField("rule_version")
    private String ruleVersion;

    /** 来源批次 ID */
    @TableField("batch_id")
    private Long batchId;

    /** 计算时间 */
    @TableField("computed_at")
    private LocalDateTime computedAt;

    /** 规则通过率（如 '80%'） */
    @TableField("pass_rate")
    private String passRate;

    /** 最近结果时间（工具执行时间快照） */
    @TableField("last_result_at")
    private LocalDateTime lastResultAt;

    /** 结果有效期至（结果时间 + 30 天，OQ-03） */
    @TableField("valid_until")
    private LocalDateTime validUntil;
}
