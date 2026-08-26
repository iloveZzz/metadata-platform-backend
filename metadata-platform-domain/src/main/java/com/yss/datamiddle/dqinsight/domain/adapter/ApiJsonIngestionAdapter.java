package com.yss.datamiddle.dqinsight.domain.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;

import java.util.Collections;

/**
 * 通用 API 结果接入适配器（application/json，DQResultSubmit，formatType = api）。
 */
public class ApiJsonIngestionAdapter implements IngestionAdapter {

    private final DqResultSubmitJsonParser parser;

    public ApiJsonIngestionAdapter() {
        this.parser = new DqResultSubmitJsonParser(new ObjectMapper());
    }

    ApiJsonIngestionAdapter(DqResultSubmitJsonParser parser) {
        this.parser = parser;
    }

    @Override
    public FormatType formatType() {
        return FormatType.API;
    }

    @Override
    public IngestParseResult parse(String rawContent) {
        IngestParseResult parsed = parser.parse(rawContent, FormatType.API);
        if (parsed.isSuccess() && parsed.getBatch().getSourceTool() != SourceTool.GENERIC) {
            return IngestParseResult.failure(FormatType.API, parsed.getBatch().getSourceTool(),
                    parsed.getBatch().getBatchNo(), DqErrorCodes.FORMAT_INVALID, ErrorCategory.FORMAT,
                    Collections.singletonList(FieldErrorItem.of("sourceTool", DqErrorCodes.FORMAT_INVALID,
                            "sourceTool 与格式类型不匹配：api 格式要求 generic")));
        }
        return parsed;
    }
}
