package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.client.vo.IngestionReceiptVO;
import com.yss.datamiddle.dqinsight.core.service.IngestionAppService;
import com.yss.datamiddle.dqinsight.core.service.convertor.IngestionReceiptConvertor;
import com.yss.datamiddle.dqinsight.domain.adapter.ApiJsonIngestionAdapter;
import com.yss.datamiddle.dqinsight.domain.adapter.CsvIngestionAdapter;
import com.yss.datamiddle.dqinsight.domain.adapter.GeJsonIngestionAdapter;
import com.yss.datamiddle.dqinsight.domain.adapter.IngestParseResult;
import com.yss.datamiddle.dqinsight.domain.adapter.IngestionAdapter;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.BatchDuplicateException;
import com.yss.datamiddle.dqinsight.domain.exception.IngestValidationException;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreCalculationTrigger;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.util.IngestErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 结果接入用例编排实现（Application 只编排）。
 *
 * <p>解析成功即入库（与资产关联解耦，SB-05）；(sourceTool, batchNo) 唯一约束兜底并发，重复批次 409；
 * 资产 ID 未命中写 pending 挂待关联队列不阻断验收；关联命中发布健康分计算触发（切片 02 seam）；
 * 审计 ingest / parse-fail 独立 append-only。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionAppServiceImpl implements IngestionAppService {

    /** 单批次规则结果上限（SB-10 已确认 5 万条，M3 决策） */
    public static final int MAX_ROWS_PER_BATCH = 50_000;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_CSV = "text/csv";

    private final DqResultGateway dqResultGateway;
    private final BatchPersistenceService batchPersistenceService;
    private final CatalogAclGateway catalogAclGateway;
    private final AuditLogGateway auditLogGateway;
    private final HealthScoreCalculationTrigger healthScoreCalculationTrigger;
    private final IngestionReceiptConvertor ingestionReceiptConvertor;

    private final GeJsonIngestionAdapter geJsonAdapter = new GeJsonIngestionAdapter();
    private final ApiJsonIngestionAdapter apiJsonAdapter = new ApiJsonIngestionAdapter();
    private final CsvIngestionAdapter csvAdapter = new CsvIngestionAdapter();

    @Override
    public IngestionReceiptVO ingest(String rawBody, String contentType, String authChannelId) {
        IngestionAdapter adapter = resolveAdapter(contentType, rawBody);
        IngestParseResult parsed = adapter.parse(rawBody);
        if (!parsed.isSuccess()) {
            persistParseFailed(parsed, authChannelId);
            throw new IngestValidationException(parsed.getErrorCode(), parsed.getErrorCategory(),
                    IngestErrorMessages.summary(parsed.getErrorCategory(), parsed.getFieldErrors()),
                    parsed.getFieldErrors());
        }

        DQResultBatch batch = parsed.getBatch();
        if (authChannelId != null && !authChannelId.isEmpty()) {
            batch.setChannelId(authChannelId);
        }
        // 批次行数上限（> 5 万 → 413，禁止静默截断）
        batch.validateRowCount(MAX_ROWS_PER_BATCH);

        // 资产关联解析（与入库解耦：未命中 pending；网络失败 422 network 分类）
        List<AssetLinkage> linkages;
        try {
            linkages = resolveLinkages(batch, parsed.getRows());
        } catch (IngestValidationException e) {
            if (e.getErrorCategory() == ErrorCategory.NETWORK) {
                persistNetworkParseFailed(batch, e, authChannelId);
            }
            throw e;
        }
        batch.setLinkageStatus(LinkageState.resolve(linkages));

        // 幂等去重：唯一约束兜底并发，重复批次 409（禁止先查后插）
        DQResultBatch saved = saveOrDuplicate(batch, parsed.getRows(), linkages);

        // 关联命中 → 健康分计算触发（切片 02 seam）
        List<String> linkedAssetIds = new ArrayList<>();
        for (AssetLinkage linkage : linkages) {
            if (linkage.getState() == LinkageState.LINKED) {
                linkedAssetIds.add(linkage.getSourceAssetId());
            }
        }
        if (!linkedAssetIds.isEmpty()) {
            healthScoreCalculationTrigger.triggerForAssets(String.valueOf(saved.getId()), linkedAssetIds);
        }

        // 审计（独立 append-only，不参与批次事务）
        auditLogGateway.record(AuditLogEntry.ingest(operator(authChannelId), saved.getBatchNo(),
                "sourceTool=" + saved.getSourceTool().getCode() + ", formatType=" + saved.getFormatType().getCode()
                        + ", rowCount=" + saved.getRowCount()));

        return ingestionReceiptConvertor.toVO(saved);
    }

    /**
     * 按 Content-Type 路由适配器（格式分类 format）。
     */
    private IngestionAdapter resolveAdapter(String contentType, String rawBody) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IngestValidationException(DqErrorCodes.FORMAT_INVALID, ErrorCategory.FORMAT,
                    "缺少 Content-Type",
                    Collections.singletonList(FieldErrorItem.of("Content-Type", DqErrorCodes.FORMAT_INVALID,
                            "缺少 Content-Type，仅支持 application/json / text/csv")));
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith(CONTENT_TYPE_JSON)) {
            // ge / api 由 payload 内 sourceTool 决定（M2：外部推送统一按 DQResultSubmit）
            SourceToolDetector detector = new SourceToolDetector(rawBody);
            if (detector.isGreatExpectations()) {
                return geJsonAdapter;
            }
            return apiJsonAdapter;
        }
        if (normalized.startsWith(CONTENT_TYPE_CSV)) {
            return csvAdapter;
        }
        throw new IngestValidationException(DqErrorCodes.FORMAT_INVALID, ErrorCategory.FORMAT,
                "不支持的 Content-Type：" + contentType,
                Collections.singletonList(FieldErrorItem.of("Content-Type", DqErrorCodes.FORMAT_INVALID,
                        "不支持的 Content-Type，仅支持 application/json / text/csv")));
    }

    /**
     * 资产关联解析（distinct 资产 ID；命中 → linked；404 → pending 挂待关联队列；超时 → network 422）。
     */
    private List<AssetLinkage> resolveLinkages(DQResultBatch batch, List<RuleResultRow> rows) {
        Set<String> assetIds = new LinkedHashSet<>();
        for (RuleResultRow row : rows) {
            if (row.getAssetId() != null && !row.getAssetId().trim().isEmpty()) {
                assetIds.add(row.getAssetId().trim());
            }
        }
        if (assetIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AssetLinkage> linkages = new ArrayList<>(assetIds.size());
        for (String assetId : assetIds) {
            AssetLookupResult lookup = catalogAclGateway.lookupAsset(assetId);
            switch (lookup.getType()) {
                case FOUND:
                    linkages.add(AssetLinkage.linked(batch, assetId, lookup.getSnapshot()));
                    break;
                case NOT_FOUND:
                    linkages.add(AssetLinkage.pending(batch, assetId));
                    break;
                case NETWORK_FAILURE:
                    throw new IngestValidationException(DqErrorCodes.NETWORK_TIMEOUT, ErrorCategory.NETWORK,
                            "资产校验服务不可用（网络超时）：" + assetId,
                            Collections.singletonList(FieldErrorItem.of("assetId",
                                    DqErrorCodes.NETWORK_TIMEOUT, "资产校验服务不可用（网络超时）")));
                default:
                    break;
            }
        }
        return linkages;
    }

    /**
     * 保存批次；重复批次（唯一约束冲突）→ 409 err.dq.batch.duplicate。
     */
    private DQResultBatch saveOrDuplicate(DQResultBatch batch, List<RuleResultRow> rows,
            List<AssetLinkage> linkages) {
        try {
            return batchPersistenceService.save(batch, rows, linkages);
        } catch (DuplicateKeyException e) {
            log.warn("批次幂等冲突（唯一约束兜底）: sourceTool={}, batchNo={}",
                    batch.getSourceTool().getCode(), batch.getBatchNo());
            throw new BatchDuplicateException(batch);
        }
    }

    /**
     * 解析失败（format / auth / network）落 dq_batch parse-failed + 审计 parse-fail（数据架构 §7）。
     */
    private void persistParseFailed(IngestParseResult parsed, String channelId) {
        String errorMessage = IngestErrorMessages.summary(parsed.getErrorCategory(), parsed.getFieldErrors());
        DQResultBatch failed = DQResultBatch.createParseFailed(parsed.getFormatType(), parsed.getSourceTool(),
                parsed.getBatchNo(), channelId, parsed.getErrorCategory(), errorMessage);
        saveOrDuplicate(failed, Collections.emptyList(), Collections.emptyList());
        auditLogGateway.record(AuditLogEntry.parseFail(operator(channelId), failed.getBatchNo(), errorMessage));
    }

    /**
     * 网络失败（资产校验超时）落 dq_batch parse-failed（network 分类）+ 审计 parse-fail。
     */
    private void persistNetworkParseFailed(DQResultBatch parsedBatch, IngestValidationException e,
            String channelId) {
        String errorMessage = IngestErrorMessages.summary(e.getErrorCategory(), e.getFieldErrors());
        DQResultBatch failed = DQResultBatch.createParseFailed(parsedBatch.getFormatType(),
                parsedBatch.getSourceTool(), parsedBatch.getBatchNo(), channelId,
                ErrorCategory.NETWORK, errorMessage);
        saveOrDuplicate(failed, Collections.emptyList(), Collections.emptyList());
        auditLogGateway.record(AuditLogEntry.parseFail(operator(channelId), failed.getBatchNo(), errorMessage));
    }

    private static String operator(String authChannelId) {
        return authChannelId == null || authChannelId.isEmpty() ? "system" : "channel:" + authChannelId;
    }

    /**
     * JSON payload 来源工具探测（避免先完整解析再路由）。
     */
    private static final class SourceToolDetector {

        private final String sourceTool;

        SourceToolDetector(String rawBody) {
            this.sourceTool = extractSourceTool(rawBody);
        }

        boolean isGreatExpectations() {
            return "great-expectations".equals(sourceTool);
        }

        private static String extractSourceTool(String rawBody) {
            if (rawBody == null || rawBody.isEmpty()) {
                return null;
            }
            int index = rawBody.indexOf("\"sourceTool\"");
            if (index < 0) {
                return null;
            }
            int colon = rawBody.indexOf(':', index);
            if (colon < 0) {
                return null;
            }
            int quoteStart = rawBody.indexOf('"', colon);
            if (quoteStart < 0) {
                return null;
            }
            int quoteEnd = rawBody.indexOf('"', quoteStart + 1);
            if (quoteEnd < 0) {
                return null;
            }
            return rawBody.substring(quoteStart + 1, quoteEnd);
        }
    }
}
