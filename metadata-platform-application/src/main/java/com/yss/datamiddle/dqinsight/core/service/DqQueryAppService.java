package com.yss.datamiddle.dqinsight.core.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.query.AuditLogPageQuery;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.AuditLogVO;
import com.yss.datamiddle.dqinsight.client.vo.DashboardVO;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;

public interface DqQueryAppService {

    PageResult<AssetHealthRowVO> pageAssetHealth(HealthScorePageQuery query);

    AssetHealthDetailVO getAssetHealthDetail(String assetId);

    RuleDetailVO getRuleDetail(String assetId, String fieldName);

    DashboardVO getDashboard(HealthScorePageQuery query);

    PageResult<AuditLogVO> pageAuditLogs(AuditLogPageQuery query);

    PageResult<IngestionRecordVO> pageIngestionRecords(IngestionRecordPageQuery query);
}
