package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 批次持久化服务（批次 + 规则明细 + 关联（pending）单聚合事务边界）。
 *
 * <p>独立 Bean 以保证 @Transactional 代理生效；UNIQUE(source_tool, batch_no) 唯一约束兜底并发重复推送
 * （C20，禁止先查后插）；审计独立 append-only 不参与本事务。</p>
 */
@Service
@RequiredArgsConstructor
public class BatchPersistenceService {

    private final DqResultGateway dqResultGateway;

    @Transactional(rollbackFor = Exception.class)
    public DQResultBatch save(DQResultBatch batch, List<RuleResultRow> rows, List<AssetLinkage> linkages) {
        return dqResultGateway.save(batch, rows, linkages);
    }
}
