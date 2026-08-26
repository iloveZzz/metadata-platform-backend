package com.yss.smartdiscovery.domain.gateway;

import com.yss.smartdiscovery.domain.audit.SmartTagAuditLog;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {
    void save(SmartTagAuditLog auditLog);
    List<SmartTagAuditLog> listLogs();
    Optional<SmartTagAuditLog> findByBatchId(String batchId);
    void update(SmartTagAuditLog auditLog);
}
