package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.AuditLogPageQuery;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.client.vo.DashboardStatsVO;
import com.yss.datamiddle.dqinsight.client.vo.DashboardVO;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.core.service.DqQueryAppService;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DashboardGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DqQueryAppServiceImpl implements DqQueryAppService {

    private final HealthScoreGateway healthScoreGateway;
    private final DashboardGateway dashboardGateway;
    private final AuditLogGateway auditLogGateway;
    private final DqResultGateway dqResultGateway;

    @Override
    public PageResult<AssetHealthRowVO> pageAssetHealth(HealthScorePageQuery query) {
        List<AssetHealthRowVO> records = healthScoreGateway.listAssetHealth(query);
        return HealthScoreGateway.toPage(records, query);
    }

    @Override
    public AssetHealthDetailVO getAssetHealthDetail(String assetId) {
        return healthScoreGateway.findAssetHealthDetail(assetId);
    }

    @Override
    public RuleDetailVO getRuleDetail(String assetId, String fieldName) {
        return healthScoreGateway.findRuleDetail(assetId, fieldName);
    }

    @Override
    public DashboardVO getDashboard(HealthScorePageQuery query) {
        DashboardStatsVO stats = dashboardGateway.loadStats(query);
        List<AssetHealthRowVO> assets = healthScoreGateway.listAssetHealth(query);
        DashboardVO vo = new DashboardVO();
        vo.setStats(stats);
        vo.setAssets(HealthScoreGateway.toPage(assets, query));
        return vo;
    }

    @Override
    public PageResult<AuditLogVO> pageAuditLogs(AuditLogPageQuery query) {
        List<AuditLogVO> records = auditLogGateway.page(query);
        return AuditLogGateway.toPage(records, query);
    }

    @Override
    public PageResult<IngestionRecordVO> pageIngestionRecords(IngestionRecordPageQuery query) {
        List<IngestionRecordVO> records = dqResultGateway.listIngestionRecords(query);
        return DqResultGateway.toPage(records, query);
    }
}
