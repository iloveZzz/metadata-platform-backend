package com.yss.datamiddle.dqinsight.domain.model;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.BatchDuplicateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批次幂等去重领域测试（WU2）。
 *
 * <p>去重键 (sourceTool, batchNo) 为 MVP 行为基线（SB-09）；并发兜底由 dq_batch
 * UNIQUE(source_tool, batch_no) 唯一约束承担（C20），禁止先查后插——重复批次 409 err.dq.batch.duplicate。</p>
 */
class IngestionIdempotencyTest {

    @Test
    void duplicateBatchMapsTo409DuplicateCode() {
        DQResultBatch first = DQResultBatch.createIngested("batch-1", SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, "ch-1", Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());
        DQResultBatch duplicate = DQResultBatch.createIngested("batch-1", SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, "ch-1", Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());

        BatchDuplicateException exception = new BatchDuplicateException(duplicate);

        assertThat(exception.getErrCode()).isEqualTo(DqErrorCodes.BATCH_DUPLICATE);
        assertThat(exception.getSourceTool()).isEqualTo(first.getSourceTool().getCode());
        assertThat(exception.getBatchNo()).isEqualTo(first.getBatchNo());
    }

    @Test
    void sameBatchNoWithDifferentSourceToolIsNotDuplicate() {
        DQResultBatch geBatch = DQResultBatch.createIngested("batch-1", SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, null, Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());
        DQResultBatch genericBatch = DQResultBatch.createIngested("batch-1", SourceTool.GENERIC,
                FormatType.API, null, Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());

        assertThat(geBatch.getSourceTool()).isNotEqualTo(genericBatch.getSourceTool());
        assertThat(geBatch.getBatchNo()).isEqualTo(genericBatch.getBatchNo());
        // 去重键为 (sourceTool, batchNo) 组合
        assertThat(geBatch.getSourceTool().getCode() + ":" + geBatch.getBatchNo())
                .isNotEqualTo(genericBatch.getSourceTool().getCode() + ":" + genericBatch.getBatchNo());
    }

    @Test
    void dedupKeyIdentityComesFromSourceToolAndBatchNo() {
        DQResultBatch batch = DQResultBatch.createIngested("batch-9", SourceTool.GENERIC,
                FormatType.CSV, null, Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());

        assertThat(batch.getBatchNo()).isEqualTo("batch-9");
        assertThat(batch.getSourceTool()).isEqualTo(SourceTool.GENERIC);
    }

    @Test
    void linkageStateResolveAggregatesBatchLevelLinkageStatus() {
        assertThat(LinkageState.resolve(null)).isEqualTo(LinkageState.NONE);
        assertThat(LinkageState.resolve(Collections.emptyList())).isEqualTo(LinkageState.NONE);
    }
}
