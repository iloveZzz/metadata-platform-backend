package com.yss.metadata.domain.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 自然语言找数意图解析结果（VO）
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryIntent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提取的核心业务关键词（如 "高净值", "交易"）
     */
    private List<String> keywords;

    /**
     * 目标数据域限定（可选，如 "crm", "trade"）
     */
    private String targetDomain;

    /**
     * 意图摘要说明
     */
    private String intentSummary;

    /**
     * 是否经过降级处理（模型超时或不可用）
     */
    private boolean fallback;
}
