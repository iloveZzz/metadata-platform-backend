package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.integration.gateway.OpenLineageEventGateway;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import com.yss.metadata.repository.OpenLineageEventRepository;
import com.yss.metadata.infrastructure.convertor.OpenLineageEventConvertor;
import com.yss.metadata.repository.entity.OpenLineageEventPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * OpenLineage 事件记录仓储实现（MyBatis-Plus；openlineage_event 追加记录 + 统计）。
 */
@Repository
public class OpenLineageEventGatewayImpl implements OpenLineageEventGateway {

    private final OpenLineageEventRepository openLineageEventRepository;
    private final OpenLineageEventConvertor openLineageEventConvertor;

    @Autowired
    public OpenLineageEventGatewayImpl(OpenLineageEventRepository openLineageEventRepository) {
        this(openLineageEventRepository, Mappers.getMapper(OpenLineageEventConvertor.class));
    }

    public OpenLineageEventGatewayImpl(OpenLineageEventRepository openLineageEventRepository, OpenLineageEventConvertor openLineageEventConvertor) {
        this.openLineageEventRepository = openLineageEventRepository;
        this.openLineageEventConvertor = openLineageEventConvertor != null ? openLineageEventConvertor : Mappers.getMapper(OpenLineageEventConvertor.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(OpenLineageEventRecord record) {
        openLineageEventRepository.insert(openLineageEventConvertor.toPO(record));
    }

    @Override
    @Transactional(readOnly = true)
    public OpenLineageStats stats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long recent24h = openLineageEventRepository.selectCount(
                Wrappers.<OpenLineageEventPO>lambdaQuery()
                        .ge(OpenLineageEventPO::getReceivedAt, since));
        long total = openLineageEventRepository.selectCount(null);
        long parsed = openLineageEventRepository.selectCount(
                Wrappers.<OpenLineageEventPO>lambdaQuery()
                        .eq(OpenLineageEventPO::getParseStatus, OpenLineageParseStatus.PARSED.getValue()));
        double rate = total == 0 ? 0.0 : (double) parsed / total;
        return OpenLineageStats.builder()
                .recent24hCount(recent24h)
                .parseSuccessRate(rate)
                .build();
    }
}
