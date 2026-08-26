package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.LinkageMapDTO;
import com.yss.datamiddle.dqinsight.client.dto.query.PendingLinkagePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.client.vo.LinkageResultVO;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;
import com.yss.datamiddle.dqinsight.core.service.LinkageAppService;
import com.yss.datamiddle.dqinsight.core.service.convertor.LinkageConvertor;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.AssetNotFoundException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelValidationException;
import com.yss.datamiddle.dqinsight.domain.exception.IngestValidationException;
import com.yss.datamiddle.dqinsight.domain.exception.LinkageAlreadyLinkedException;
import com.yss.datamiddle.dqinsight.domain.exception.LinkageNotFoundException;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreCalculationTrigger;
import com.yss.datamiddle.dqinsight.domain.gateway.LinkageGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.LinkageMatchMode;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 资产关联治理用例编排实现（Application 只编排，C10；映射单聚合事务，C26）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkageAppServiceImpl implements LinkageAppService {

    private static final String DEFAULT_OPERATOR = "system";

    private final LinkageGateway linkageGateway;
    private final CatalogAclGateway catalogAclGateway;
    private final DqResultGateway dqResultGateway;
    private final HealthScoreCalculationTrigger healthScoreCalculationTrigger;
    private final AuditLogGateway auditLogGateway;
    private final LinkageConvertor linkageConvertor;

    @Override
    public PageResult<PendingLinkageVO> listPending(PendingLinkagePageQuery query) {
        List<PendingLinkageVO> records = linkageGateway.listPending(query);
        return PageResult.of(records, query.getTempTotalCount(), query.getPageSize(), query.getPageIndex());
    }

    @Override
    @Transactional
    public LinkageResultVO mapLinkage(Long linkageId, LinkageMapDTO dto, String operator) {
        String targetAssetId = dto == null ? null : dto.getAssetId();
        if (targetAssetId == null || targetAssetId.trim().isEmpty()) {
            throw new ChannelValidationException(DqErrorCodes.ASSET_NOT_FOUND, "目标资产 ID 必填",
                    Collections.singletonList(FieldErrorItem.of("assetId", DqErrorCodes.ASSET_NOT_FOUND,
                            "目标资产 ID 必填")));
        }
        AssetLinkage linkage = linkageGateway.findById(linkageId);
        if (linkage == null) {
            throw new LinkageNotFoundException(linkageId);
        }
        if (linkage.getState() == LinkageState.LINKED && !dto.isConfirmOverwrite()) {
            throw new LinkageAlreadyLinkedException(linkageId);
        }

        // 防腐层校验目标资产（C26：只读消费冻结 GET /api/assets；不存在 422）
        AssetLookupResult lookup = catalogAclGateway.lookupAsset(targetAssetId.trim());
        if (lookup.getType() == AssetLookupResult.LookupType.NOT_FOUND) {
            throw new AssetNotFoundException(targetAssetId);
        }
        if (lookup.getType() == AssetLookupResult.LookupType.NETWORK_FAILURE) {
            throw new IngestValidationException(DqErrorCodes.NETWORK_TIMEOUT, ErrorCategory.NETWORK,
                    "资产校验服务不可用（网络超时）：" + targetAssetId,
                    Collections.singletonList(FieldErrorItem.of("assetId", DqErrorCodes.NETWORK_TIMEOUT,
                            "资产校验服务不可用（网络超时）")));
        }
        AssetSnapshot snapshot = lookup.getSnapshot();

        // 保存资产名称与数据域快照（人工映射，C26）
        linkage.setResolvedAssetId(snapshot.getAssetId());
        linkage.setAssetName(snapshot.getAssetName());
        linkage.setDomain(snapshot.getDomain());
        linkage.setAssetType(snapshot.getAssetType());
        linkage.setMatchMode(LinkageMatchMode.MANUAL);
        linkage.setState(LinkageState.LINKED);
        linkage.setMappedAt(Instant.now());
        linkage.setMappedBy(operatorOf(operator));
        linkage.setNote("人工映射");
        linkageGateway.save(linkage);

        // 触发健康分首次计算（复用切片 02 计算入口，C30 seam）；
        // 与切片 01 调用约定一致：传源资产 ID（rule 结果按源 ID 分组），健康行按快照解析 ID 落主平台口径
        healthScoreCalculationTrigger.triggerForAssets(
                String.valueOf(linkage.getBatchId()), Collections.singletonList(linkage.getSourceAssetId()));

        // 审计 linkage-map（SB-08；object = 来源批次号，与 ingest / health-calc 审计约定一致）
        String batchNo = batchNoOf(linkage.getBatchId());
        auditLogGateway.record(AuditLogEntry.linkageMap(operatorOf(operator), batchNo,
                "sourceAssetId=" + linkage.getSourceAssetId() + ", resolvedAssetId=" + snapshot.getAssetId()));
        return linkageConvertor.toVO(linkage);
    }

    private String batchNoOf(Long batchId) {
        if (batchId == null) {
            return "";
        }
        com.yss.datamiddle.dqinsight.domain.model.DQResultBatch batch = dqResultGateway.findBatchById(batchId);
        return batch == null ? "" : batch.getBatchNo();
    }

    private static String operatorOf(String operator) {
        return operator == null || operator.isEmpty() ? DEFAULT_OPERATOR : operator;
    }
}
