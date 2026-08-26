package com.yss.datamiddle.dqinsight.domain.model;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.BatchTooLargeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批次行数上限领域测试（WU2 / C29：> 5 万条 → 413 err.dq.batch.too-large，禁止静默截断）。
 */
class IngestionRowLimitTest {

    private static final int MAX_ROWS_PER_BATCH = 50_000;

    @Test
    void batchWithinLimitPassesValidation() {
        DQResultBatch batch = buildBatch(MAX_ROWS_PER_BATCH);
        batch.validateRowCount(MAX_ROWS_PER_BATCH);
        assertThat(batch.getRowCount()).isEqualTo(MAX_ROWS_PER_BATCH);
    }

    @Test
    void batchOverLimitThrowsTooLargeException() {
        DQResultBatch batch = buildBatch(MAX_ROWS_PER_BATCH + 1);

        assertThatThrownBy(() -> batch.validateRowCount(MAX_ROWS_PER_BATCH))
                .isInstanceOf(BatchTooLargeException.class)
                .satisfies(e -> {
                    BatchTooLargeException ex = (BatchTooLargeException) e;
                    assertThat(ex.getErrCode()).isEqualTo(DqErrorCodes.BATCH_TOO_LARGE);
                    assertThat(ex.getActualRowCount()).isEqualTo(MAX_ROWS_PER_BATCH + 1);
                    assertThat(ex.getMaxRowsPerBatch()).isEqualTo(MAX_ROWS_PER_BATCH);
                });
    }

    private static DQResultBatch buildBatch(int rowCount) {
        List<RuleResultRow> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(RuleResultRow.builder()
                    .assetId("asset-" + i)
                    .ruleName("r" + i)
                    .ruleType(RuleType.FORMAT)
                    .status(RuleStatus.PASSED)
                    .executionTime(Instant.parse("2026-08-11T10:00:00Z"))
                    .build());
        }
        return DQResultBatch.createIngested("batch-limit", SourceTool.GENERIC, FormatType.CSV, null,
                Instant.parse("2026-08-11T10:00:00Z"), rows);
    }
}
