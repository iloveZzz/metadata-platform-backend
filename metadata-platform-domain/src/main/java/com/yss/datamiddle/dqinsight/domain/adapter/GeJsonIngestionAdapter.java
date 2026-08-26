package com.yss.datamiddle.dqinsight.domain.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;

import java.util.Collections;

/**
 * GE 结果接入适配器（application/json，DQResultSubmit，formatType = ge）。
 *
 * <p>契约假设（Freeze 记录 M2）：外部推送统一按 DQResultSubmit / CSV schema；
 * GE 原生格式仅由通道拉取侧适配层转换后再入库（适配层职责边界）。</p>
 */
public class GeJsonIngestionAdapter implements IngestionAdapter {

    private final DqResultSubmitJsonParser parser;

    public GeJsonIngestionAdapter() {
        this.parser = new DqResultSubmitJsonParser(new ObjectMapper());
    }

    GeJsonIngestionAdapter(DqResultSubmitJsonParser parser) {
        this.parser = parser;
    }

    @Override
    public FormatType formatType() {
        return FormatType.GE;
    }

    @Override
    public IngestParseResult parse(String rawContent) {
        IngestParseResult parsed = parser.parse(rawContent, FormatType.GE);
        if (parsed.isSuccess() && parsed.getBatch().getSourceTool() != SourceTool.GREAT_EXPECTATIONS) {
            return IngestParseResult.failure(FormatType.GE, parsed.getBatch().getSourceTool(),
                    parsed.getBatch().getBatchNo(), DqErrorCodes.FORMAT_INVALID, ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("sourceTool", DqErrorCodes.FORMAT_INVALID,
                            "sourceTool 与格式类型不匹配：ge 格式要求 great-expectations")));
        }
        return parsed;
    }
}
