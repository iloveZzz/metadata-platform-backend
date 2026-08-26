package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.dqinsight.client.dto.query.AuditLogPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.repository.DqAuditLogRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqAuditLogConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计网关实现（dq_audit_log 只读不可变 append-only，仅 INSERT；查询只读分页）。
 */
@Repository("dqAuditLogGatewayImpl")
@RequiredArgsConstructor
public class AuditLogGatewayImpl implements AuditLogGateway {

    private final DqAuditLogRepository dqAuditLogRepository;
    private final DqAuditLogConvertor dqAuditLogConvertor;

    @Override
    public void record(AuditLogEntry entry) {
        dqAuditLogRepository.insert(dqAuditLogConvertor.toPO(entry));
    }

    @Override
    public List<AuditLogVO> page(AuditLogPageQuery query) {
        LambdaQueryWrapper<DqAuditLogPO> wrapper = Wrappers.lambdaQuery();
        if (query.getAction() != null) {
            wrapper.eq(DqAuditLogPO::getAction, query.getAction().getCode());
        }
        // 时间倒序 + (action, event_time DESC) 复合索引（V4 迁移），满足审计查询筛选 / 排序
        wrapper.orderByDesc(DqAuditLogPO::getEventTime);
        com.baomidou.mybatisplus.core.metadata.IPage<DqAuditLogPO> page = dqAuditLogRepository.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageIndex(), query.getPageSize()), wrapper);
        query.setTempTotalCount(page.getTotal());
        return page.getRecords().stream()
                .map(dqAuditLogConvertor::toVO)
                .collect(Collectors.toList());
    }
}
