package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 审计记录（AuditLogEntry，dq_audit_log 只读不可变 append-only）。
 *
 * <p>审计独立写、不参与批次事务（数据架构 §7）。object 字段存对象引用（批次号等）。</p>
 */
@Getter
public class AuditLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发生时间 */
    private final Instant eventTime;

    /** 操作者（外部通道推送为通道标识 / system） */
    private final String operator;

    /** 动作（7 类枚举） */
    private final AuditAction action;

    /** 对象引用（批次号等） */
    private final String object;

    /** 结果 */
    private final AuditResult result;

    /** 详情（脱敏） */
    private final String detail;

    private AuditLogEntry(Instant eventTime, String operator, AuditAction action, String object,
            AuditResult result, String detail) {
        this.eventTime = eventTime;
        this.operator = operator;
        this.action = action;
        this.object = object;
        this.result = result;
        this.detail = detail;
    }

    /** 接入成功审计 */
    public static AuditLogEntry ingest(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.INGEST, object, AuditResult.SUCCESS, detail);
    }

    /** 解析失败审计 */
    public static AuditLogEntry parseFail(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.PARSE_FAIL, object, AuditResult.FAILURE, detail);
    }

    /** 健康分计算审计（记录计算规则版本与结果，DQI-002 人工审查点） */
    public static AuditLogEntry healthCalc(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.HEALTH_CALC, object, AuditResult.SUCCESS, detail);
    }

    /** 通道配置变更审计（新建 / 更新配置，SB-08） */
    public static AuditLogEntry channelConfig(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.CHANNEL_CONFIG, object, AuditResult.SUCCESS, detail);
    }

    /** 通道启停审计（停用需二次确认，SB-08） */
    public static AuditLogEntry channelToggle(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.CHANNEL_TOGGLE, object, AuditResult.SUCCESS, detail);
    }

    /** 通道重试拉取审计（结果 success / failure，SB-08） */
    public static AuditLogEntry channelRetry(String operator, String object, String detail, AuditResult result) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.CHANNEL_RETRY, object, result, detail);
    }

    /** 资产关联人工映射审计（含目标资产与来源批次，SB-08） */
    public static AuditLogEntry linkageMap(String operator, String object, String detail) {
        return new AuditLogEntry(Instant.now(), operator, AuditAction.LINKAGE_MAP, object, AuditResult.SUCCESS, detail);
    }
}
