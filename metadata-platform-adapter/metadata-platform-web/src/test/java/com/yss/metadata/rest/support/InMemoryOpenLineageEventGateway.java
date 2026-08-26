package com.yss.metadata.rest.support;

import com.yss.metadata.domain.integration.gateway.OpenLineageEventGateway;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.integration.model.OpenLineageStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OpenLineage 事件记录仓储内存实现（Web 契约测试 seam；镜像统计语义）。
 */
public class InMemoryOpenLineageEventGateway implements OpenLineageEventGateway {

    private final List<OpenLineageEventRecord> store = new ArrayList<>();

    @Override
    public void save(OpenLineageEventRecord record) {
        store.add(record);
    }

    public List<OpenLineageEventRecord> all() {
        return Collections.unmodifiableList(store);
    }

    @Override
    public OpenLineageStats stats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long recent24h = store.stream()
                .filter(record -> record.getReceivedAt() != null && !record.getReceivedAt().isBefore(since))
                .count();
        long parsed = store.stream()
                .filter(record -> record.getParseStatus() == OpenLineageParseStatus.PARSED)
                .count();
        double rate = store.isEmpty() ? 0.0 : (double) parsed / store.size();
        return OpenLineageStats.builder()
                .recent24hCount(recent24h)
                .parseSuccessRate(rate)
                .build();
    }
}
