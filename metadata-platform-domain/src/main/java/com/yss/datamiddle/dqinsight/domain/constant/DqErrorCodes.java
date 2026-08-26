package com.yss.datamiddle.dqinsight.domain.constant;

/**
 * DQ Insight 业务错误码（冻结 OpenAPI 错误码约定，v0.1.0-frozen）。
 */
public final class DqErrorCodes {

    private DqErrorCodes() {
    }

    /** 接入解析格式错误（422，format 分类） */
    public static final String FORMAT_INVALID = "err.dq.format.invalid";

    /** CSV schema 违反（422，format 分类，fieldErrors 携带行号 row:N） */
    public static final String CSV_SCHEMA = "err.dq.csv.schema";

    /** 认证失败（422，auth 分类；错误信息脱敏） */
    public static final String AUTH_INVALID = "err.dq.auth.invalid";

    /** 网络超时（422，network 分类） */
    public static final String NETWORK_TIMEOUT = "err.dq.network.timeout";

    /** 重复批次（409，幂等去重 (sourceTool, batchNo) 唯一约束兜底） */
    public static final String BATCH_DUPLICATE = "err.dq.batch.duplicate";

    /** 批次行数超限（413，> 5 万条） */
    public static final String BATCH_TOO_LARGE = "err.dq.batch.too-large";

    /** 无权限（403，数据域外不可见） */
    public static final String FORBIDDEN = "err.dq.forbidden";

    /** 资源不存在（404，健康分 / 规则明细无对应资产时） */
    public static final String NOT_FOUND = "err.dq.not-found";

    /** 通道重名（409，name 未删除唯一约束兜底，C25） */
    public static final String CHANNEL_NAME_CONFLICT = "err.dq.channel.name-conflict";

    /** 通道状态冲突（409：拉取中重复触发 / 拉取中更新配置与启停 / 停用通道重试，C25 幂等） */
    public static final String CHANNEL_BUSY = "err.dq.channel.busy";

    /** 通道删除引用冲突（409：存在历史接入结果，结果引用必须可追溯） */
    public static final String CHANNEL_IN_USE = "err.dq.channel.in-use";

    /** 目标资产不存在（422：防腐层消费冻结 GET /api/assets 校验，C26） */
    public static final String ASSET_NOT_FOUND = "err.dq.asset.not-found";

    /** 关联批次已关联（409：覆盖需 confirmOverwrite=true + 二次确认，C26） */
    public static final String LINKAGE_ALREADY_LINKED = "err.dq.linkage.already-linked";
}
