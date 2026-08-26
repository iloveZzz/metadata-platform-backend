package com.yss.metadata.domain.ai.gateway;

import com.yss.metadata.domain.ai.model.QueryIntent;

/**
 * 大模型与语义理解防腐网关端口
 *
 * <p>硬门禁要求：构造的 Prompt 严格限制仅包含元数据 Schema、注释与业务术语，绝不传输业务数据行。
 *
 * @author ai
 * @since 2026-08-15
 */
public interface LlmClientGateway {

    /**
     * 解析用户自然语言找数意图
     *
     * @param query 用户提问
     * @param domainFilter 可选限定数据域
     * @return 意图解析结果
     */
    QueryIntent parseIntent(String query, String domainFilter);

    /**
     * 基于召回资产生成自然语言解答总结
     *
     * @param query 用户提问
     * @param matchedAssetNames 匹配到的资产名称列表
     * @return 自然语言解答
     */
    String generateSummaryReply(String query, java.util.List<String> matchedAssetNames);
}
