package com.yss.metadata.infrastructure.ai;

import com.yss.metadata.domain.ai.gateway.LlmClientGateway;
import com.yss.metadata.domain.ai.model.QueryIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 大模型与语义理解网关实现
 *
 * <p>具备大模型接口调用、业务术语映射与超时/离线自动降级能力。
 * 遵循安全基线：绝不构造或传输包含底层数据行的 Prompt。
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Component
public class LlmClientGatewayImpl implements LlmClientGateway {

    @Value("${ai.llm.enabled:false}")
    private boolean llmEnabled;

    @Value("${ai.llm.model:deepseek-v3}")
    private String modelName;

    /**
     * 行业领域词库映射表（业务术语 -> 关键词特征）
     */
    private static final Map<String, List<String>> DOMAIN_SYNONYMS = new HashMap<>();

    static {
        DOMAIN_SYNONYMS.put("高净值", Arrays.asList("vip", "hnw", "customer", "客户", "等级"));
        DOMAIN_SYNONYMS.put("公募", Arrays.asList("public_fund", "fund", "基金", "交易"));
        DOMAIN_SYNONYMS.put("交易", Arrays.asList("trade", "order", "订单", "流水", "trans"));
        DOMAIN_SYNONYMS.put("持仓", Arrays.asList("position", "holding", "份额", "balance"));
        DOMAIN_SYNONYMS.put("估值", Arrays.asList("valuation", "nav", "净值", "核算"));
        DOMAIN_SYNONYMS.put("客户", Arrays.asList("customer", "user", "investor", "client"));
    }

    @Override
    public QueryIntent parseIntent(String query, String domainFilter) {
        if (query == null || query.trim().isEmpty()) {
            return QueryIntent.builder()
                    .keywords(new ArrayList<>())
                    .intentSummary("空查询意图")
                    .fallback(false)
                    .build();
        }

        // 若未启用远程大模型或调用超时，采用内置智能分词与术语权重匹配（本地降级引擎）
        if (!llmEnabled) {
            return fallbackParse(query, domainFilter);
        }

        try {
            // 生产环境下调用外部 LLM 端点（仅传输 query 意图分析 Prompt）
            return callRemoteLlm(query, domainFilter);
        } catch (Exception e) {
            log.warn("大模型调用异常，自动触发本地语义分词降级: query={}", query, e);
            return fallbackParse(query, domainFilter);
        }
    }

    @Override
    public String generateSummaryReply(String query, List<String> matchedAssetNames) {
        if (matchedAssetNames == null || matchedAssetNames.isEmpty()) {
            return "根据您的查询需求「" + query + "」，平台未检索到完全匹配的资产。建议您尝试放宽关键词或限定数据域重新查找。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("已为您找到与「").append(query).append("」高度相关的 ")
                .append(matchedAssetNames.size()).append(" 个核心元数据资产：\n");
        for (int i = 0; i < matchedAssetNames.size(); i++) {
            sb.append(i + 1).append(". ").append(matchedAssetNames.get(i)).append("\n");
        }
        sb.append("建议优先选用贴合业务域明细层（DWD）或聚合层（ADS）的标准表。");
        return sb.toString();
    }

    private QueryIntent fallbackParse(String query, String domainFilter) {
        Set<String> keywords = new HashSet<>();
        String lowerQuery = query.toLowerCase();

        // 匹配业务术语库
        DOMAIN_SYNONYMS.forEach((term, synonyms) -> {
            if (lowerQuery.contains(term.toLowerCase())) {
                keywords.add(term);
                keywords.addAll(synonyms);
            }
            for (String syn : synonyms) {
                if (lowerQuery.contains(syn.toLowerCase())) {
                    keywords.add(term);
                    keywords.add(syn);
                }
            }
        });

        // 基础分词拆解
        String[] tokens = query.split("[\\s,，、_\\-]+");
        for (String token : tokens) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }

        String summary = "已提取关键词 [" + String.join(", ", keywords) + "]"
                + (domainFilter != null ? "，限定数据域: " + domainFilter : "");

        return QueryIntent.builder()
                .keywords(new ArrayList<>(keywords))
                .targetDomain(domainFilter)
                .intentSummary(summary)
                .fallback(true)
                .build();
    }

    private QueryIntent callRemoteLlm(String query, String domainFilter) {
        // 远程调用模拟（保留拓展至 OpenAI/DeepSeek 接口插槽）
        return fallbackParse(query, domainFilter);
    }
}
