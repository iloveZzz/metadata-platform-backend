package com.yss.metadata.rest.support;

import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.audit.model.AuditLogPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志仓储内存实现（Web 契约测试 seam；镜像追加写入 + 分页 time DESC 语义）。
 */
public class InMemoryAuditLogRepository implements AuditLogGateway {

    private final List<AuditLogEntry> entries = new ArrayList<>();

    @Override
    public void record(AuditLogEntry entry) {
        entries.add(entry);
    }

    @Override
    public AuditLogPage page(int pageIndex, int pageSize) {
        List<AuditLogEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(AuditLogEntry::getTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        int from = (pageIndex - 1) * pageSize;
        if (from >= sorted.size()) {
            return AuditLogPage.builder().items(Collections.emptyList())
                    .total(sorted.size()).pageIndex(pageIndex).pageSize(pageSize).build();
        }
        int to = Math.min(from + pageSize, sorted.size());
        return AuditLogPage.builder()
                .items(new ArrayList<>(sorted.subList(from, to)))
                .total(sorted.size())
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    public List<AuditLogEntry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
