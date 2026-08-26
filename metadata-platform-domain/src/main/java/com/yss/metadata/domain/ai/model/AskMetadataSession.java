package com.yss.metadata.domain.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 智能找数会话结果聚合根 / VO
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskMetadataSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自然语言原始提问
     */
    private String query;

    /**
     * AI 针对提问的结构化回复解答
     */
    private String reply;

    /**
     * 意图摘要
     */
    private String queryIntent;

    /**
     * 匹配召回的资产卡片列表
     */
    private List<MatchedAssetCard> matchedAssets;

    /**
     * 是否降级为本地检索
     */
    private boolean isFallback;
}
