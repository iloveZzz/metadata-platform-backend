package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.SecurityAuditLog;

import java.util.List;

public interface SecurityAuditGateway {
    Long save(SecurityAuditLog log);
    List<SecurityAuditLog> findPage(int pageIndex, int pageSize, String actionType, String operator, String riskLevel);
    long countPage(String actionType, String operator, String riskLevel);
}
