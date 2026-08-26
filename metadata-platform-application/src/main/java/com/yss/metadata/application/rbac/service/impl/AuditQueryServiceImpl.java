package com.yss.metadata.application.rbac.service.impl;

import com.yss.metadata.application.rbac.service.AuditQueryService;
import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.client.vo.AuditLogVO;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计查询应用服务实现（WU-06-02；只读不可变，time DESC 分页）。
 */
@Service
@RequiredArgsConstructor
public class AuditQueryServiceImpl implements AuditQueryService {

    private final AuditLogGateway auditLogGateway;
    private final RbacAppConvertor rbacAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public AuditPage page(int pageIndex, int pageSize) {
        AuditLogPage page = auditLogGateway.page(pageIndex, pageSize);
        List<AuditLogVO> items = page.getItems().stream()
                .map(rbacAppConvertor::toAuditLogVO)
                .collect(Collectors.toList());
        return new AuditPage(items, page.getTotal());
    }
}
