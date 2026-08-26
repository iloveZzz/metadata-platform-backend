package com.yss.datamiddle.dqinsight.domain.adapter;

import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GE / API JSON（DQResultSubmit）解析领域测试（WU1：结果接入解析与错误分类）。
 */
class IngestionJsonParsingTest {

    private final GeJsonIngestionAdapter geAdapter = new GeJsonIngestionAdapter();
    private final ApiJsonIngestionAdapter apiAdapter = new ApiJsonIngestionAdapter();

    @Test
    void geJsonParsesToIngestedBatch() {
        String json = "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"ge-batch-001\","
                + "\"executionTime\":\"2026-08-11T10:00:00+08:00\",\"channelId\":\"ch-1\","
                + "\"assetId\":\"asset-1\",\"results\":[{\"fieldName\":\"name\",\"ruleName\":\"非空率\","
                + "\"ruleType\":\"non-null-rate\",\"status\":\"passed\",\"failureReason\":null}]}";

        IngestParseResult result = geAdapter.parse(json);

        assertThat(result.isSuccess()).isTrue();
        DQResultBatch batch = result.getBatch();
        assertThat(batch.getFormatType()).isEqualTo(FormatType.GE);
        assertThat(batch.getSourceTool()).isEqualTo(SourceTool.GREAT_EXPECTATIONS);
        assertThat(batch.getBatchNo()).isEqualTo("ge-batch-001");
        assertThat(batch.getChannelId()).isEqualTo("ch-1");
        assertThat(batch.getStatus()).isEqualTo(IngestionStatus.INGESTED);
        assertThat(batch.getRowCount()).isEqualTo(1);
        assertThat(batch.getValidUntil()).isEqualTo(batch.getExecutionTime().plus(30, ChronoUnit.DAYS));
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().get(0).getAssetId()).isEqualTo("asset-1");
        assertThat(result.getRows().get(0).getRuleType()).isEqualTo(RuleType.NON_NULL_RATE);
        assertThat(result.getRows().get(0).getStatus()).isEqualTo(RuleStatus.PASSED);
    }

    @Test
    void apiJsonParsesToIngestedBatch() {
        String json = "{\"sourceTool\":\"generic\",\"batchNo\":\"api-batch-001\","
                + "\"executionTime\":\"2026-08-11T10:00:00Z\",\"results\":[{\"ruleName\":\"格式\","
                + "\"ruleType\":\"format\",\"status\":\"warn\",\"failureReason\":\"格式不规范\"}]}";

        IngestParseResult result = apiAdapter.parse(json);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBatch().getFormatType()).isEqualTo(FormatType.API);
        assertThat(result.getBatch().getSourceTool()).isEqualTo(SourceTool.GENERIC);
        assertThat(result.getRows().get(0).getStatus()).isEqualTo(RuleStatus.WARN);
    }

    @Test
    void missingRequiredFieldsProduceFieldErrors() {
        String json = "{\"sourceTool\":\"great-expectations\",\"results\":[]}";

        IngestParseResult result = geAdapter.parse(json);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("batchNo", "executionTime");
    }

    @Test
    void invalidEnumsProduceFieldLevelErrors() {
        String json = "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"b-1\","
                + "\"executionTime\":\"2026-08-11T10:00:00Z\",\"results\":[{\"ruleName\":\"r\","
                + "\"ruleType\":\"bad-type\",\"status\":\"bad-status\"}]}";

        IngestParseResult result = geAdapter.parse(json);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("results.0.ruleType", "results.0.status");
    }

    @Test
    void invalidIso8601ExecutionTimeProducesFieldError() {
        String json = "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"b-1\","
                + "\"executionTime\":\"2026-13-99\",\"results\":[{\"ruleName\":\"r\","
                + "\"ruleType\":\"format\",\"status\":\"passed\"}]}";

        IngestParseResult result = geAdapter.parse(json);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("executionTime");
    }

    @Test
    void malformedJsonProducesFormatError() {
        IngestParseResult result = geAdapter.parse("{not-json");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
    }

    @Test
    void geAdapterRejectsNonGeSourceTool() {
        String json = "{\"sourceTool\":\"generic\",\"batchNo\":\"b-1\","
                + "\"executionTime\":\"2026-08-11T10:00:00Z\",\"results\":[{\"ruleName\":\"r\","
                + "\"ruleType\":\"format\",\"status\":\"passed\"}]}";

        IngestParseResult result = geAdapter.parse(json);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("sourceTool");
    }

    @Test
    void apiAdapterRejectsNonGenericSourceTool() {
        String json = "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"b-1\","
                + "\"executionTime\":\"2026-08-11T10:00:00Z\",\"results\":[{\"ruleName\":\"r\","
                + "\"ruleType\":\"format\",\"status\":\"passed\"}]}";

        IngestParseResult result = apiAdapter.parse(json);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("sourceTool");
    }
}
