package com.yss.smartdiscovery.infrastructure.persistence;

import com.yss.smartdiscovery.domain.audit.SmartTagAuditLog;
import com.yss.smartdiscovery.domain.gateway.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final Map<String, SmartTagAuditLog> auditMap = new ConcurrentHashMap<>();

    public InMemoryAuditLogRepository() {
        initDefaultLogs();
    }

    private void initDefaultLogs() {
        auditMap.put("BATCH-20260817-001", SmartTagAuditLog.builder()
                .id("AUDIT-01")
                .batchId("BATCH-20260817-001")
                .actionType("AUTO_APPLY")
                .actionName("高置信自动生效")
                .operator("System (LLM Funnel)")
                .fieldCount(15)
                .status("APPLIED")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
    }

    @Override
    public void save(SmartTagAuditLog auditLog) {
        auditMap.put(auditLog.getBatchId(), auditLog);
    }

    @Override
    public List<SmartTagAuditLog> listLogs() {
        return new ArrayList<>(auditMap.values());
    }

    @Override
    public Optional<SmartTagAuditLog> findByBatchId(String batchId) {
        return Optional.ofNullable(auditMap.get(batchId));
    }

    @Override
    public void update(SmartTagAuditLog auditLog) {
        auditMap.put(auditLog.getBatchId(), auditLog);
    }
}
