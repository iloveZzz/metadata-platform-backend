package com.yss.datamiddle.dqinsight.domain.adapter;

import com.yss.datamiddle.dqinsight.domain.model.FormatType;

/**
 * 接入适配器 SPI（外部 DQ 工具格式隔离，DQI-001 / C18）。
 *
 * <p>实现：ge（GE 结果 JSON，DQResultSubmit）/ csv（通用 CSV，SB-04 schema）/ api（通用 API 结果 JSON，DQResultSubmit）。</p>
 */
public interface IngestionAdapter {

    /**
     * 本适配器支持的格式类型。
     */
    FormatType formatType();

    /**
     * 解析外部结果内容。
     *
     * @param rawContent 原始内容（JSON / CSV 文本）
     * @return 解析结果（成功含批次与规则明细；失败含错误分类与字段级错误，错误信息脱敏）
     */
    IngestParseResult parse(String rawContent);
}
