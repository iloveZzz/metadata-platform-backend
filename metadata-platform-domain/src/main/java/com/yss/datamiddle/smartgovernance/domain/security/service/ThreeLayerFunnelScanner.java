package com.yss.datamiddle.smartgovernance.domain.security.service;

import com.yss.datamiddle.smartgovernance.domain.llm.LlmGateway;
import com.yss.datamiddle.smartgovernance.domain.llm.LlmInferenceResult;
import com.yss.datamiddle.smartgovernance.domain.llm.PromptPayload;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;
import com.yss.datamiddle.smartgovernance.domain.security.model.FunnelLayer;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 数据安全三层漏斗识别调度器 (L1正则 -> L2词典向量 -> L3大模型推理)
 */
public class ThreeLayerFunnelScanner {

    private final LlmGateway llmGateway;

    public ThreeLayerFunnelScanner(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    /**
     * 对单字段执行三层漏斗识别
     */
    public SensitiveCandidate scanColumn(
            SecurityTemplate template,
            String dataSource,
            String databaseName,
            String tableName,
            String tableComment,
            String columnName,
            String columnComment,
            String dataType,
            List<String> neighborColumns
    ) {
        // 1. L1 正则预筛
        if (template.getRules() != null) {
            for (ClassificationRule rule : template.getRules()) {
                if (Boolean.TRUE.equals(rule.getIsActive()) && rule.getRegexPattern() != null && !rule.getRegexPattern().isEmpty()) {
                    try {
                        Pattern pattern = Pattern.compile(rule.getRegexPattern(), Pattern.CASE_INSENSITIVE);
                        if (pattern.matcher(columnName).find() || (columnComment != null && pattern.matcher(columnComment).find())) {
                            return buildCandidate(template.getId(), rule.getId(), dataSource, databaseName, tableName,
                                    columnName, columnComment, dataType, rule.getSensitiveType(), rule.getSecurityLevel(),
                                    rule.getClauseRef(), "命中规则正则特征: " + rule.getSensitiveName(),
                                    new BigDecimal("0.98"), FunnelLayer.L1_REGEX, template);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 2. L2 词典匹配
        if (template.getRules() != null) {
            for (ClassificationRule rule : template.getRules()) {
                if (Boolean.TRUE.equals(rule.getIsActive()) && rule.getDictionaryWords() != null && !rule.getDictionaryWords().isEmpty()) {
                    List<String> words = Arrays.asList(rule.getDictionaryWords().split("[,，|]"));
                    for (String word : words) {
                        String trimWord = word.trim();
                        if (!trimWord.isEmpty()) {
                            if (columnName.toLowerCase().contains(trimWord.toLowerCase())
                                    || (columnComment != null && columnComment.toLowerCase().contains(trimWord.toLowerCase()))) {
                                return buildCandidate(template.getId(), rule.getId(), dataSource, databaseName, tableName,
                                        columnName, columnComment, dataType, rule.getSensitiveType(), rule.getSecurityLevel(),
                                        rule.getClauseRef(), "命中敏感词典词条: " + trimWord,
                                        new BigDecimal("0.88"), FunnelLayer.L2_VECTOR, template);
                            }
                        }
                    }
                }
            }
        }

        // 3. L3 大模型少样本上下文推理
        PromptPayload payload = PromptPayload.builder()
                .databaseName(databaseName)
                .tableName(tableName)
                .tableComment(tableComment)
                .columnName(columnName)
                .columnComment(columnComment)
                .dataType(dataType)
                .neighborColumnNames(neighborColumns)
                .standardTemplateCode(template.getTemplateCode())
                .build();

        ZeroDataLeakAssertion.assertSafePayload(payload);

        LlmInferenceResult inference = llmGateway.inferSecurityClassification(payload);
        if (inference != null && inference.getSecurityLevel() != null) {
            return buildCandidate(template.getId(), null, dataSource, databaseName, tableName,
                    columnName, columnComment, dataType, inference.getSensitiveType(), inference.getSecurityLevel(),
                    inference.getClauseRef(), inference.getReasoning(), inference.getConfidence(),
                    FunnelLayer.L3_LLM, template);
        }

        // 兜底返回 L1 公开级别
        return buildCandidate(template.getId(), null, dataSource, databaseName, tableName,
                columnName, columnComment, dataType, "GENERAL_DATA", SecurityLevel.L1,
                "通用国标", "未检测出敏感特征", new BigDecimal("0.95"),
                FunnelLayer.L1_REGEX, template);
    }

    private SensitiveCandidate buildCandidate(
            String templateId,
            String ruleId,
            String dataSource,
            String databaseName,
            String tableName,
            String columnName,
            String columnComment,
            String dataType,
            String sensitiveType,
            SecurityLevel level,
            String clauseRef,
            String reasoning,
            BigDecimal confidence,
            FunnelLayer layer,
            SecurityTemplate template
    ) {
        boolean autoApprove = Boolean.TRUE.equals(template.getDefaultAutoApproval())
                && template.getDefaultThreshold() != null
                && confidence.compareTo(template.getDefaultThreshold()) >= 0;

        return SensitiveCandidate.builder()
                .id(UUID.randomUUID().toString())
                .templateId(templateId)
                .ruleId(ruleId)
                .dataSource(dataSource)
                .databaseName(databaseName)
                .tableName(tableName)
                .columnName(columnName)
                .columnComment(columnComment)
                .dataType(dataType)
                .sensitiveType(sensitiveType)
                .recommendedLevel(level)
                .clauseRef(clauseRef)
                .reasoning(reasoning)
                .confidence(confidence)
                .funnelLayer(layer)
                .status(autoApprove ? CandidateStatus.APPROVED : CandidateStatus.PENDING)
                .actualLevel(autoApprove ? level : null)
                .operator(autoApprove ? "SYSTEM_AUTO_LLM" : null)
                .reviewComment(autoApprove ? "系统依据超高置信度自动采纳生效" : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
