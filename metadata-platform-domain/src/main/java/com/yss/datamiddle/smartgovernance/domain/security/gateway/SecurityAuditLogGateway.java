package com.yss.datamiddle.smartgovernance.domain.security.gateway;

import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityAuditLog;

import java.util.List;
import java.util.Optional;

public interface SecurityAuditLogGateway {
    void save(SecurityAuditLog log);

    void batchSave(List<SecurityAuditLog> logs);

    Optional<SecurityAuditLog> findById(String id);

    List<SecurityAuditLog> queryLogs(Integer pageIndex, Integer pageSize, String keyword);

    long countLogs(String keyword);
}
