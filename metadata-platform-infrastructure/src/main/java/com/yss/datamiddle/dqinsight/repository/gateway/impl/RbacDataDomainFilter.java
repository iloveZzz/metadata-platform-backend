package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 数据域过滤 RBAC 实现（切片 05 落地，替换切片 03 的 DefaultDataDomainFilter 占位，C24）。
 *
 * <p>当前用户可见数据域来自 {@code dq.rbac.visible-domains}（MVP 配置源）；空 = 不限制
 * （与切片 03 seam 语义一致：空集合 = 不做域限制）。真实 RBAC（主平台当前用户域解析，
 * OQ-05）接入后替换本实现或注入解析端口（人工审查点）。</p>
 */
@Repository
@RequiredArgsConstructor
public class RbacDataDomainFilter implements DataDomainFilter {

    private final DqRbacProperties rbacProperties;

    @Override
    public List<String> visibleDomains() {
        List<String> visible = rbacProperties.getVisibleDomains();
        return visible == null ? Collections.emptyList() : visible;
    }
}
