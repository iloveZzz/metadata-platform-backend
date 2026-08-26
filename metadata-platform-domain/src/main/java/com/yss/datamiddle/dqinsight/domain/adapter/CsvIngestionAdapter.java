package com.yss.datamiddle.dqinsight.domain.adapter;

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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 CSV 导入适配器（SB-04 schema，formatType = csv）。
 *
 * <p>每行一条规则结果；必填 asset_id / rule_name / rule_type / status / execution_time；
 * 可选 field_name（空 = 资产级规则）/ failure_reason / batch_no（缺省由平台生成）。
 * 字段级错误 field = "row:N"（CSV 行号）。</p>
 */
public class CsvIngestionAdapter implements IngestionAdapter {

    private static final String BOM = "\uFEFF";

    private static final String[] REQUIRED_HEADERS = {"asset_id", "rule_name", "rule_type", "status",
            "execution_time"};

    @Override
    public FormatType formatType() {
        return FormatType.CSV;
    }

    @Override
    public IngestParseResult parse(String rawContent) {
        String content = stripBom(rawContent);
        if (content == null || content.trim().isEmpty()) {
            return IngestParseResult.failure(FormatType.CSV, SourceTool.GENERIC, null, DqErrorCodes.CSV_SCHEMA,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("row:1", DqErrorCodes.CSV_SCHEMA, "CSV 内容为空")));
        }

        List<FieldErrorItem> errors = new ArrayList<>();
        Map<String, Integer> headerIndex = null;
        List<CSVRecord> dataRecords = new ArrayList<>();
        try {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(new StringReader(content));
            for (CSVRecord record : parser) {
                if (headerIndex == null) {
                    headerIndex = buildHeaderIndex(record);
                    validateHeader(headerIndex, errors);
                } else if (record.size() > 0 && !isBlankRecord(record)) {
                    dataRecords.add(record);
                }
            }
        } catch (IOException e) {
            return IngestParseResult.failure(FormatType.CSV, SourceTool.GENERIC, null, DqErrorCodes.CSV_SCHEMA,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("row:1", DqErrorCodes.CSV_SCHEMA, "CSV 解析失败")));
        }

        if (headerIndex == null) {
            errors.add(FieldErrorItem.of("row:1", DqErrorCodes.CSV_SCHEMA, "CSV 缺少表头行"));
        }
        if (!errors.isEmpty()) {
            return IngestParseResult.failure(FormatType.CSV, SourceTool.GENERIC, null, DqErrorCodes.CSV_SCHEMA,
                    ErrorCategory.FORMAT, errors);
        }

        List<RuleResultRow> rows = new ArrayList<>();
        String batchNo = null;
        for (CSVRecord record : dataRecords) {
            String rowNo = "row:" + record.getRecordNumber();
            RuleResultRow.RuleResultRowBuilder builder = RuleResultRow.builder();

            String assetId = value(record, headerIndex, "asset_id");
            if (isBlank(assetId)) {
                errors.add(FieldErrorItem.of(rowNo + ".asset_id", DqErrorCodes.CSV_SCHEMA, "asset_id 必填"));
            } else {
                builder.assetId(assetId.trim());
            }

            String ruleName = value(record, headerIndex, "rule_name");
            if (isBlank(ruleName)) {
                errors.add(FieldErrorItem.of(rowNo + ".rule_name", DqErrorCodes.CSV_SCHEMA, "rule_name 必填"));
            } else {
                builder.ruleName(ruleName.trim());
            }

            String ruleTypeCode = value(record, headerIndex, "rule_type");
            RuleType ruleType = RuleType.fromCodeOrNull(ruleTypeCode);
            if (ruleType == null) {
                errors.add(FieldErrorItem.of(rowNo + ".rule_type", DqErrorCodes.CSV_SCHEMA,
                        "rule_type 枚举不合法：仅支持 non-null-rate / format / uniqueness / value-range / freshness"));
            } else {
                builder.ruleType(ruleType);
            }

            String statusCode = value(record, headerIndex, "status");
            RuleStatus ruleStatus = RuleStatus.fromCodeOrNull(statusCode);
            if (ruleStatus == null) {
                errors.add(FieldErrorItem.of(rowNo + ".status", DqErrorCodes.CSV_SCHEMA,
                        "status 枚举不合法：仅支持 passed / warn / failed / error"));
            } else {
                builder.status(ruleStatus);
            }

            String executionTimeText = value(record, headerIndex, "execution_time");
            Instant executionTime = IsoTimes.parse(executionTimeText);
            if (executionTime == null) {
                errors.add(FieldErrorItem.of(rowNo + ".execution_time", DqErrorCodes.CSV_SCHEMA,
                        "execution_time 必须为 ISO 8601 时间"));
            } else {
                builder.executionTime(executionTime);
            }

            String fieldName = value(record, headerIndex, "field_name");
            builder.fieldName(isBlank(fieldName) ? null : fieldName.trim());
            String failureReason = value(record, headerIndex, "failure_reason");
            builder.failureReason(isBlank(failureReason) ? null : failureReason.trim());

            String rowBatchNo = value(record, headerIndex, "batch_no");
            if (isBlank(rowBatchNo) && batchNo == null) {
                batchNo = null;
            } else if (!isBlank(rowBatchNo) && batchNo == null) {
                batchNo = rowBatchNo.trim();
            }

            if (ruleName != null && ruleType != null && ruleStatus != null && executionTime != null
                    && !isBlank(assetId)) {
                rows.add(builder.build());
            }
        }

        if (!errors.isEmpty()) {
            return IngestParseResult.failure(FormatType.CSV, SourceTool.GENERIC, batchNo, DqErrorCodes.CSV_SCHEMA,
                    ErrorCategory.FORMAT, errors);
        }
        if (rows.isEmpty()) {
            return IngestParseResult.failure(FormatType.CSV, SourceTool.GENERIC, batchNo, DqErrorCodes.CSV_SCHEMA,
                    ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("row:2", DqErrorCodes.CSV_SCHEMA,
                            "CSV 无数据行（至少一行规则结果）")));
        }

        Instant batchExecutionTime = rows.get(0).getExecutionTime();
        String resolvedBatchNo = batchNo == null
                ? DQResultBatch.generatePlatformBatchNo(FormatType.CSV) : batchNo;
        DQResultBatch batch = DQResultBatch.createIngested(
                resolvedBatchNo, SourceTool.GENERIC, FormatType.CSV, null,
                batchExecutionTime, rows);
        return IngestParseResult.success(batch, rows);
    }

    private Map<String, Integer> buildHeaderIndex(CSVRecord header) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i);
            if (name != null && !name.trim().isEmpty()) {
                index.put(name.trim().toLowerCase(java.util.Locale.ROOT), i);
            }
        }
        return index;
    }

    private void validateHeader(Map<String, Integer> headerIndex, List<FieldErrorItem> errors) {
        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                errors.add(FieldErrorItem.of("row:1", DqErrorCodes.CSV_SCHEMA, "CSV 缺少必需列：" + required));
            }
        }
    }

    private static String value(CSVRecord record, Map<String, Integer> headerIndex, String column) {
        Integer index = headerIndex.get(column);
        if (index == null || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isBlankRecord(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            String cell = record.get(i);
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String stripBom(String content) {
        if (content == null) {
            return null;
        }
        return content.startsWith(BOM) ? content.substring(1) : content;
    }
}
