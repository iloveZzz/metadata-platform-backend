package com.yss.datamiddle.dqinsight.domain.gateway;

/**
 * 操作类权限端口（切片 05，OQ-05 对齐主平台 RBAC 概念、独立实现）。
 *
 * <p>操作类端点（通道新建 / 配置 / 启停 / 重试、人工映射、审计查询）无权限时
 * 越权调用 403 err.dq.forbidden 兜底（DQI-007，不泄露域外资源存在性）。
 * MVP 实现由配置驱动（RbacOperationPermission），真实 RBAC 由主平台接入后替换
 * （当前用户上下文 starter 未入脚手架，人工审查点）。</p>
 */
public interface OperationPermissionPort {

    /**
     * 当前用户是否具备指定操作能力（能力码见 {@code DqCapabilities}）。
     */
    boolean canOperate(String capability);
}
