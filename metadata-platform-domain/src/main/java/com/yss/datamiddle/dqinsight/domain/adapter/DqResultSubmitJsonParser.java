package com.yss.datamiddle.dqinsight.domain.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DQResultSubmit（冻结 OpenAPI application/json 接入 schema）JSON 解析核心。
 *
 * <p>由 ge / api 两个适配器复用；字段级错误路径使用冻结约定（如 results.3.ruleType）。</p>
 */
final class DqResultSubmitJsonParser {

    private final ObjectMapper objectMapper;

    DqResultSubmitJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 DQResultSubmit JSON。
     *
     * @param content    JSON 字符串
     * @param formatType 本适配器格式类型（ge / api）
     */
    IngestParseResult parse(String content, FormatType formatType) {
        if (content == null || content.trim().isEmpty()) {
            return IngestParseResult.failure(formatType, null, null, DqErrorCodes.FORMAT_INVALID,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("body", DqErrorCodes.FORMAT_INVALID,
                            "请求体为空")));
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            return IngestParseResult.failure(formatType, null, null, DqErrorCodes.FORMAT_INVALID,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("body", DqErrorCodes.FORMAT_INVALID,
                            "请求体不是合法 JSON")));
        }
        if (root == null || !root.isObject()) {
            return IngestParseResult.failure(formatType, null, null, DqErrorCodes.FORMAT_INVALID,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("body", DqErrorCodes.FORMAT_INVALID,
                            "请求体必须是 JSON 对象")));
        }

        List<FieldErrorItem> errors = new ArrayList<>();
        SourceTool sourceTool = SourceTool.fromCodeOrNull(requiredText(root, "sourceTool", errors));
        String batchNo = requiredText(root, "batchNo", errors);
        Instant executionTime = requiredTime(root, "executionTime", errors);
        String channelId = optionalText(root, "channelId");
        String assetId = optionalText(root, "assetId");
        List<RuleResultRow> rows = parseResults(root, assetId, executionTime, errors);

        if (!errors.isEmpty()) {
            return IngestParseResult.failure(formatType, sourceTool, batchNo, DqErrorCodes.FORMAT_INVALID,
                    ErrorCategory.FORMAT, errors);
        }
        DQResultBatch batch = DQResultBatch.createIngested(batchNo, sourceTool, formatType, channelId,
                executionTime, rows);
        return IngestParseResult.success(batch, rows);
    }

    private List<RuleResultRow> parseResults(JsonNode root, String assetId, Instant batchExecutionTime,
            List<FieldErrorItem> errors) {
        JsonNode resultsNode = root.get("results");
        if (resultsNode == null) {
            errors.add(FieldErrorItem.of("results", DqErrorCodes.FORMAT_INVALID, "results 必填"));
            return Collections.emptyList();
        }
        if (!resultsNode.isArray()) {
            errors.add(FieldErrorItem.of("results", DqErrorCodes.FORMAT_INVALID, "results 必须是数组"));
            return Collections.emptyList();
        }
        List<RuleResultRow> rows = new ArrayList<>();
        int index = 0;
        for (JsonNode item : resultsNode) {
            String prefix = "results." + index;
            if (!item.isObject()) {
                errors.add(FieldErrorItem.of(prefix, DqErrorCodes.FORMAT_INVALID, "规则结果必须是对象"));
                index++;
                continue;
            }
            String ruleName = requiredText(item, "ruleName", prefix, errors);
            RuleType ruleType = RuleType.fromCodeOrNull(requiredText(item, "ruleType", prefix, errors));
            if (ruleType == null && !hasError(prefix + ".ruleType", errors)) {
                errors.add(FieldErrorItem.of(prefix + ".ruleType", DqErrorCodes.FORMAT_INVALID,
                        "ruleType 枚举不合法：仅支持 non-null-rate / format / uniqueness / value-range / freshness"));
            }
            RuleStatus ruleStatus = RuleStatus.fromCodeOrNull(requiredText(item, "status", prefix, errors));
            if (ruleStatus == null && !hasError(prefix + ".status", errors)) {
                errors.add(FieldErrorItem.of(prefix + ".status", DqErrorCodes.FORMAT_INVALID,
                        "status 枚举不合法：仅支持 passed / warn / failed / error"));
            }
            String fieldName = optionalText(item, "fieldName");
            String failureReason = optionalText(item, "failureReason");
            if (ruleName != null && ruleType != null && ruleStatus != null) {
                RuleResultRow row = RuleResultRow.builder()
                        .assetId(blankToNull(assetId))
                        .fieldName(blankToNull(fieldName))
                        .ruleName(ruleName)
                        .ruleType(ruleType)
                        .status(ruleStatus)
                        .failureReason(blankToNull(failureReason))
                        .executionTime(batchExecutionTime)
                        .build();
                rows.add(row);
            }
            index++;
        }
        return rows;
    }

    private static String requiredText(JsonNode node, String field, List<FieldErrorItem> errors) {
        return requiredText(node, field, field, errors);
    }

    private static String requiredText(JsonNode node, String field, String fieldPath,
            List<FieldErrorItem> errors) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().trim().isEmpty()) {
            errors.add(FieldErrorItem.of(fieldPath, DqErrorCodes.FORMAT_INVALID, field + " 必填"));
            return null;
        }
        return value.asText().trim();
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        return value.asText();
    }

    private static Instant requiredTime(JsonNode node, String field, List<FieldErrorItem> errors) {
        String text = requiredText(node, field, errors);
        if (text == null) {
            return null;
        }
        Instant parsed = IsoTimes.parse(text);
        if (parsed == null) {
            errors.add(FieldErrorItem.of(field, DqErrorCodes.FORMAT_INVALID,
                    field + " 必须为 ISO 8601 时间"));
            return null;
        }
        return parsed;
    }

    private static boolean hasError(String fieldPath, List<FieldErrorItem> errors) {
        for (FieldErrorItem error : errors) {
            if (fieldPath.equals(error.getField())) {
                return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
