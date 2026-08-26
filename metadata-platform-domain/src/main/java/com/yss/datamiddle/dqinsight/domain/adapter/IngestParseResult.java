package com.yss.datamiddle.dqinsight.domain.adapter;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 接入解析结果。
 *
 * <p>失败时携带错误分类（format / auth / network）、错误码与字段级错误（CSV 行号 row:N）；错误信息脱敏。</p>
 */
@Getter
public class IngestParseResult {

    private final boolean success;

    /** 解析成功的批次（失败时为 null） */
    private final DQResultBatch batch;

    /** 解析成功的规则明细（失败时为空） */
    private final List<RuleResultRow> rows;

    /** 格式类型（失败时仍可恢复，用于写 parse-failed 批次） */
    private final FormatType formatType;

    /** 失败时可恢复的批次号（不可恢复为 null） */
    private final String batchNo;

    /** 失败时可恢复的来源工具（不可恢复为 null） */
    private final SourceTool sourceTool;

    /** 失败错误码（err.dq.format.invalid / err.dq.csv.schema 等） */
    private final String errorCode;

    /** 失败错误分类 */
    private final ErrorCategory errorCategory;

    /** 失败字段级错误 */
    private final List<FieldErrorItem> fieldErrors;

    private IngestParseResult(boolean success, DQResultBatch batch, List<RuleResultRow> rows,
            FormatType formatType, String batchNo, SourceTool sourceTool, String errorCode,
            ErrorCategory errorCategory, List<FieldErrorItem> fieldErrors) {
        this.success = success;
        this.batch = batch;
        this.rows = rows == null ? Collections.emptyList() : rows;
        this.formatType = formatType;
        this.batchNo = batchNo;
        this.sourceTool = sourceTool;
        this.errorCode = errorCode;
        this.errorCategory = errorCategory;
        this.fieldErrors = fieldErrors == null ? Collections.emptyList() : fieldErrors;
    }

    public static IngestParseResult success(DQResultBatch batch, List<RuleResultRow> rows) {
        return new IngestParseResult(true, batch, rows, batch.getFormatType(), batch.getBatchNo(),
                batch.getSourceTool(), null, null, null);
    }

    public static IngestParseResult failure(FormatType formatType, SourceTool sourceTool, String batchNo,
            String errorCode, ErrorCategory errorCategory, List<FieldErrorItem> fieldErrors) {
        return new IngestParseResult(false, null, new ArrayList<>(), formatType, batchNo, sourceTool,
                errorCode, errorCategory, fieldErrors);
    }
}
