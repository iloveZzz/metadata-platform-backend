package com.yss.datamiddle.dqinsight.domain.gateway;

import java.util.List;

/**
 * 数据域过滤端口（C24 安全红线，seam）。
 *
 * <p>切片 03 定义端口并接入读端点查询路径（仪表盘聚合 / 资产列表按可见数据域过滤，
 * 域外不展示）；权限解析与 403 兜底的完整实现由切片 05 落地（契约 03 seam_deferred，
 * owner = 切片 05 实现者，follow_up_ticket = issues/05-rbac-audit.md）。</p>
 *
 * <p>返回当前用户可见数据域集合；空集合 = 不限制（切片 05 落地前默认全量可见，由
 * {@code DefaultDataDomainFilter} 提供，切片 05 以真实 RBAC 实现替换）。</p>
 */
public interface DataDomainFilter {

    /**
     * 当前用户可见数据域集合；空集合 = 不做域限制。
     */
    List<String> visibleDomains();
}
