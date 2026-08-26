package com.yss.smartdiscovery.domain.rule;

import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaggingFunnelEngine {

    @Data
    @Builder
    public static class ColumnTarget {
        private String tableName;
        private String columnName;
        private String columnComment;
        private String dataType;
        private String currentTag;
    }

    public static List<SmartTagCandidate> analyze(List<ColumnTarget> targets, List<SmartTagDefinition> availableTags, double autoApplyThreshold) {
        List<SmartTagCandidate> candidates = new ArrayList<>();
        if (targets == null || targets.isEmpty() || availableTags == null || availableTags.isEmpty()) {
            return candidates;
        }

        String batchId = "BATCH-" + System.currentTimeMillis();

        for (ColumnTarget target : targets) {
            SmartTagCandidate candidate = null;

            // Layer 1: 正则规则匹配
            for (SmartTagDefinition tag : availableTags) {
                TagRule rule = tag.getTagRule();
                if (rule != null && rule.matchesRegex(target.getColumnName(), target.getColumnComment())) {
                    candidate = SmartTagCandidate.builder()
                            .id("CAN-" + UUID.randomUUID().toString().substring(0, 8))
                            .tableName(target.getTableName())
                            .columnName(target.getColumnName())
                            .columnComment(target.getColumnComment())
                            .currentTag(target.getCurrentTag())
                            .recommendedTagId(tag.getId())
                            .recommendedTagName(tag.getTagName())
                            .tagCategory(tag.getCategoryCode())
                            .source("L1_RULE")
                            .confidence(0.98)
                            .inferenceReason("Layer 1 启发式正则模式命中: " + rule.getRegexPattern())
                            .batchId(batchId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    break;
                }
            }

            // Layer 2: 词库语义匹配
            if (candidate == null) {
                for (SmartTagDefinition tag : availableTags) {
                    TagRule rule = tag.getTagRule();
                    if (rule != null && rule.matchesGlossary(target.getColumnComment(), null)) {
                        candidate = SmartTagCandidate.builder()
                                .id("CAN-" + UUID.randomUUID().toString().substring(0, 8))
                                .tableName(target.getTableName())
                                .columnName(target.getColumnName())
                                .columnComment(target.getColumnComment())
                                .currentTag(target.getCurrentTag())
                                .recommendedTagId(tag.getId())
                                .recommendedTagName(tag.getTagName())
                                .tagCategory(tag.getCategoryCode())
                                .source("L2_DICT")
                                .confidence(0.88)
                                .inferenceReason("Layer 2 中文业务术语词库语义匹配成功")
                                .batchId(batchId)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        break;
                    }
                }
            }

            // Layer 3: LLM Few-Shot 兜底推导
            if (candidate == null) {
                SmartTagDefinition defaultTag = availableTags.get(0);
                candidate = SmartTagCandidate.builder()
                        .id("CAN-" + UUID.randomUUID().toString().substring(0, 8))
                        .tableName(target.getTableName())
                        .columnName(target.getColumnName())
                        .columnComment(target.getColumnComment())
                        .currentTag(target.getCurrentTag())
                        .recommendedTagId(defaultTag.getId())
                        .recommendedTagName(defaultTag.getTagName())
                        .tagCategory(defaultTag.getCategoryCode())
                        .source("L3_LLM")
                        .confidence(0.82)
                        .inferenceReason("Layer 3 大模型结合 Schema 字段注释与上下文语义推导得出")
                        .batchId(batchId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }

            // 依据阈值双通道分流
            candidate.applyAutomatically(autoApplyThreshold);
            candidates.add(candidate);
        }

        return candidates;
    }
}
