package com.yss.datamiddle.dqinsight.domain.constant;

/**
 * 操作类能力码（切片 05 操作权限模型，OQ-05 对齐主平台 RBAC 概念、独立实现）。
 *
 * <p>读端点按数据域可见性过滤（域外隐藏）；操作类端点（通道配置 / 启停 / 重试、
 * 人工映射、审计查询）无权限禁用，越权调用 403 err.dq.forbidden 兜底（DQI-007）。
 * MVP 权限来源为配置（dq.rbac.deny-capabilities，见 DqRbacProperties），真实 RBAC
 * 接入主平台后以 OperationPermissionPort 实现替换（人工审查点，OQ-05）。</p>
 */
public final class DqCapabilities {

    private DqCapabilities() {
    }

    /** 新建接入通道 */
    public static final String CHANNEL_CREATE = "channel:create";

    /** 通道配置变更 / 启停 */
    public static final String CHANNEL_UPDATE = "channel:update";

    /** 删除通道 */
    public static final String CHANNEL_DELETE = "channel:delete";

    /** 通道重试拉取 */
    public static final String CHANNEL_RETRY = "channel:retry";

    /** 资产关联人工映射 */
    public static final String LINKAGE_MAP = "linkage:map";

    /** 审计日志查询（管理员端点） */
    public static final String AUDIT_QUERY = "audit:query";
}
