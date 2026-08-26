package com.yss.datamiddle.dqinsight.domain.adapter;

import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用 CSV 导入（SB-04 schema）解析领域测试（WU1）。
 */
class IngestionCsvParsingTest {

    private final CsvIngestionAdapter adapter = new CsvIngestionAdapter();

    private static final String VALID_HEADER = "asset_id,field_name,rule_name,rule_type,status,"
            + "failure_reason,execution_time,batch_no";

    @Test
    void validCsvParsesToIngestedBatch() {
        String csv = VALID_HEADER + "\n"
                + "asset-1,,非空率,non-null-rate,passed,,2026-08-11T10:00:00+08:00,batch-csv-1\n"
                + "asset-2,name,格式,format,warn,格式不规范,2026-08-11T10:00:00+08:00,batch-csv-1\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isTrue();
        DQResultBatch batch = result.getBatch();
        assertThat(batch.getFormatType()).isEqualTo(FormatType.CSV);
        assertThat(batch.getSourceTool()).isEqualTo(SourceTool.GENERIC);
        assertThat(batch.getBatchNo()).isEqualTo("batch-csv-1");
        assertThat(batch.getStatus()).isEqualTo(IngestionStatus.INGESTED);
        assertThat(batch.getRowCount()).isEqualTo(2);
        assertThat(result.getRows()).hasSize(2);
        assertThat(result.getRows().get(0).getFieldName()).isNull();
        assertThat(result.getRows().get(0).getAssetId()).isEqualTo("asset-1");
        assertThat(result.getRows().get(1).getFieldName()).isEqualTo("name");
    }

    @Test
    void missingBatchNoIsGeneratedByPlatform() {
        String csv = VALID_HEADER + "\n"
                + "asset-1,,非空率,non-null-rate,passed,,2026-08-11T10:00:00+08:00,\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBatch().getBatchNo()).isNotBlank();
    }

    @Test
    void missingRequiredFieldProducesRowLevelFieldError() {
        String csv = VALID_HEADER + "\n"
                + ",,非空率,non-null-rate,passed,,2026-08-11T10:00:00+08:00,b-1\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("row:2.asset_id");
    }

    @Test
    void invalidEnumsProduceRowLevelFieldErrorsWithRowNumbers() {
        String csv = VALID_HEADER + "\n"
                + "asset-1,,r,bad-rule-type,passed,,2026-08-11T10:00:00+08:00,b-1\n"
                + "asset-2,,r2,format,bad-status,,2026-08-11T10:00:00+08:00,b-1\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("row:2.rule_type", "row:3.status");
    }

    @Test
    void invalidIso8601ExecutionTimeProducesRowLevelFieldError() {
        String csv = VALID_HEADER + "\n"
                + "asset-1,,r,format,passed,,2026-13-99,b-1\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getField())
                .contains("row:2.execution_time");
    }

    @Test
    void missingRequiredHeaderProducesCsvSchemaError() {
        String csv = "asset_id,rule_name,status\n"
                + "asset-1,r,passed\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getFieldErrors())
                .extracting(f -> f.getCode())
                .contains("err.dq.csv.schema");
    }

    @Test
    void emptyContentProducesCsvSchemaError() {
        IngestParseResult result = adapter.parse("");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
    }

    @Test
    void utf8BomIsIgnored() {
        String csv = "\uFEFF" + VALID_HEADER + "\n"
                + "asset-1,,非空率,non-null-rate,passed,,2026-08-11T10:00:00+08:00,b-1\n";

        IngestParseResult result = adapter.parse(csv);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBatch().getRowCount()).isEqualTo(1);
    }
}
