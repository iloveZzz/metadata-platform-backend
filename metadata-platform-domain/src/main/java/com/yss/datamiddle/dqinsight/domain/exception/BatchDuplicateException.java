package com.yss.datamiddle.dqinsight.domain.exception;

import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;

/**
 * 批次幂等冲突（409 err.dq.batch.duplicate）。
 *
 * <p>重复推送相同 (sourceTool, batchNo) 时抛出；由 dq_batch UNIQUE(source_tool, batch_no)
 * 唯一约束兜底并发（C20），禁止先查后插。</p>
 */
public class BatchDuplicateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String sourceTool;

    private final String batchNo;

    public BatchDuplicateException(DQResultBatch batch) {
        super("批次已存在（幂等冲突）：sourceTool=" + batch.getSourceTool().getCode()
                + ", batchNo=" + batch.getBatchNo());
        this.sourceTool = batch.getSourceTool().getCode();
        this.batchNo = batch.getBatchNo();
    }

    public BatchDuplicateException(String sourceTool, String batchNo) {
        super("批次已存在（幂等冲突）：sourceTool=" + sourceTool + ", batchNo=" + batchNo);
        this.sourceTool = sourceTool;
        this.batchNo = batchNo;
    }

    public String getErrCode() {
        return DqErrorCodes.BATCH_DUPLICATE;
    }

    public String getSourceTool() {
        return sourceTool;
    }

    public String getBatchNo() {
        return batchNo;
    }
}
