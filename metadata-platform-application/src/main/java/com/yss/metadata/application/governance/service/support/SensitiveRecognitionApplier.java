package com.yss.metadata.application.governance.service.support;

import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.collector.model.SavedColumnRef;
import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.RecognizableColumn;
import com.yss.metadata.domain.governance.model.RecognizedClassification;
import com.yss.metadata.domain.governance.model.SensitiveRecognizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 敏感识别应用器（WU-04-02 候选自动识别；采集编排在 autoClassify 时调用）。
 *
 * <p>对已入库资产/列引用运行 {@link SensitiveRecognizer}（启用规则 + 内置规则），
 * 命中产出待确认候选（source=auto，status=pending），经 {@link ClassificationGateway#saveCandidate}
 * 幂等落库（同 asset+column+name 已存在跳过，重复采集不重复产候选）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensitiveRecognitionApplier {

    private final ClassRuleGateway classRuleGateway;
    private final ClassificationGateway classificationGateway;

    /**
     * 对一批已入库资产运行敏感识别并落候选（幂等）。
     *
     * @param assets 已入库资产引用（含列 id/名/注释）
     * @return 新增候选数
     */
    public int apply(List<SavedAssetRef> assets) {
        if (assets == null || assets.isEmpty()) {
            return 0;
        }
        List<ClassRule> enabledRules = classRuleGateway.findEnabled();
        int created = 0;
        for (SavedAssetRef asset : assets) {
            for (SavedColumnRef column : asset.getColumns()) {
                RecognizableColumn recognizable = RecognizableColumn.builder()
                        .assetId(asset.getAssetId())
                        .columnId(column.getColumnId())
                        .name(column.getName())
                        .comment(column.getComment())
                        .build();
                for (RecognizedClassification hit : SensitiveRecognizer.recognize(recognizable, enabledRules)) {
                    boolean inserted = classificationGateway.saveCandidate(Classification.builder()
                            .id(UUID.randomUUID().toString())
                            .assetId(asset.getAssetId())
                            .columnId(column.getColumnId())
                            .name(hit.getName())
                            .level(hit.getLevel())
                            .source("auto")
                            .status(ClassificationStatus.PENDING)
                            .build());
                    if (inserted) {
                        created++;
                    }
                }
            }
        }
        if (created > 0) {
            log.info("敏感识别落候选完成，assets={}, candidates={}", assets.size(), created);
        }
        return created;
    }
}
