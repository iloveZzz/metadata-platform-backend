package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.OperationPermissionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 操作类权限 MVP 实现（dq.rbac.deny-capabilities 配置驱动，切片 05）。
 *
 * <p>命中拒绝名单 → 无权限（403 err.dq.forbidden 兜底）；名单为空 → 全部操作允许。
 * 真实 RBAC（主平台能力 / 角色解析，OQ-05）接入后替换本实现（人工审查点）。</p>
 */
@Repository
@RequiredArgsConstructor
public class RbacOperationPermission implements OperationPermissionPort {

    private final DqRbacProperties rbacProperties;

    @Override
    public boolean canOperate(String capability) {
        if (capability == null) {
            return false;
        }
        return !rbacProperties.getDenyCapabilities().contains(capability);
    }
}
