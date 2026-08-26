package com.yss.datamiddle.dqinsight.domain.service;

import com.yss.datamiddle.dqinsight.domain.exception.DqForbiddenException;
import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import com.yss.datamiddle.dqinsight.domain.gateway.OperationPermissionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 数据域可见性与操作权限守卫（Domain 领域服务，DQI-007 / C24 安全红线）。
 *
 * <p>读端点按数据域过滤：域外资产不展示（浏览隐藏），直连域外详情 403 err.dq.forbidden
 * （错误信息不含资源标识，不泄露域外资源存在性）；操作类端点无权限时 403 兜底。
 * 域过滤语义（数据架构 §10）：以快照 domain + 当前用户可见域计算；可见域为空 = 不限制。
 * 受限用户对 domain 为 null（不可判定）的资源按域外处理（最小权限，不泄露）。</p>
 *
 * <p>MVP：可见域与操作权限来源为配置（RbacDataDomainFilter / RbacOperationPermission），
 * 真实 RBAC 由主平台接入后替换实现（OQ-05 人工审查点）。</p>
 */
@Service
@RequiredArgsConstructor
public class DataDomainGuard {

    private final DataDomainFilter dataDomainFilter;
    private final OperationPermissionPort operationPermissionPort;

    /**
     * 当前用户可见数据域（null 归一为空列表；空 = 不限制）。
     */
    public List<String> visibleDomains() {
        List<String> visible = dataDomainFilter.visibleDomains();
        return visible == null ? Collections.emptyList() : visible;
    }

    /**
     * 指定数据域是否对当前用户可见（可见域为空 = 不限制；domain 为 null 且受限 = 域外）。
     */
    public boolean canView(String domain) {
        List<String> visible = visibleDomains();
        if (visible.isEmpty()) {
            return true;
        }
        return domain != null && visible.contains(domain);
    }

    /**
     * 直连详情守卫：域外抛 403 err.dq.forbidden（消息不含资源标识，不泄露存在性）。
     */
    public void assertViewAllowed(String domain) {
        if (!canView(domain)) {
            throw new DqForbiddenException("无权限查看该资产质量结果");
        }
    }

    /**
     * 操作类端点守卫：无权限抛 403 err.dq.forbidden（能力码见 {@code DqCapabilities}）。
     */
    public void assertOperationAllowed(String capability) {
        if (operationPermissionPort == null || !operationPermissionPort.canOperate(capability)) {
            throw new DqForbiddenException("无操作权限");
        }
    }
}
