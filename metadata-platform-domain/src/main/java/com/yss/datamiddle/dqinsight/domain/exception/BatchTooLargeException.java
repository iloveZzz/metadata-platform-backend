package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;

/**
 * 批次行数超限（413 err.dq.batch.too-large，SB-10 已确认 ≤5 万条 / 批次，M3 决策）。
 *
 * <p>禁止静默截断（C29）。</p>
 */
public class BatchTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int actualRowCount;

    private final int maxRowsPerBatch;

    public BatchTooLargeException(int actualRowCount, int maxRowsPerBatch) {
        super("批次行数超过上限：" + actualRowCount + " > " + maxRowsPerBatch);
        this.actualRowCount = actualRowCount;
        this.maxRowsPerBatch = maxRowsPerBatch;
    }

    public String getErrCode() {
        return DqErrorCodes.BATCH_TOO_LARGE;
    }

    public int getActualRowCount() {
        return actualRowCount;
    }

    public int getMaxRowsPerBatch() {
        return maxRowsPerBatch;
    }
}
