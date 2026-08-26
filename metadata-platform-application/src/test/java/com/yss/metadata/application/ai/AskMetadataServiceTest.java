package com.yss.metadata.application.ai;

import com.yss.metadata.application.asset.support.InMemorySearchIndex;
import com.yss.metadata.domain.ai.gateway.AskMetadataLogGateway;
import com.yss.metadata.domain.ai.gateway.LlmClientGateway;
import com.yss.metadata.domain.ai.model.AskMetadataSession;
import com.yss.metadata.domain.ai.model.QueryIntent;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 智能找数业务逻辑与领域用例测试
 *
 * @author ai
 * @since 2026-08-15
 */
class AskMetadataServiceTest {

    private AskMetadataApplicationService service;
    private InMemorySearchIndex searchIndex;
    private List<String> loggedQueries;

    @BeforeEach
    void setUp() {
        com.yss.metadata.application.asset.support.InMemoryAssetRepository assetRepository = new com.yss.metadata.application.asset.support.InMemoryAssetRepository();
        assetRepository.seedSourceName("src-01", "MySQL-Trade");
        searchIndex = new InMemorySearchIndex(assetRepository);
        loggedQueries = new ArrayList<>();

        // 初始化测试数据
        Asset asset1 = Asset.builder()
                .id("ast-001")
                .sourceId("src-01")
                .name("dwd_trade_order_di")
                .type("table")
                .domain("trade")
                .status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.now())
                .build();
        Asset asset2 = Asset.builder()
                .id("ast-002")
                .sourceId("src-01")
                .name("dwd_customer_base_di")
                .type("table")
                .domain("crm")
                .status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.now())
                .build();

        assetRepository.save(asset1);
        assetRepository.save(asset2);



        LlmClientGateway llmGateway = new LlmClientGateway() {
            @Override
            public QueryIntent parseIntent(String query, String domainFilter) {
                if (query.contains("生僻资产")) {
                    return QueryIntent.builder()
                            .keywords(Collections.singletonList("生僻资产"))
                            .intentSummary("未命中意图")
                            .fallback(true)
                            .build();
                }
                return QueryIntent.builder()
                        .keywords(Arrays.asList("dwd", "trade", "高净值", "交易"))
                        .targetDomain(domainFilter)
                        .intentSummary("匹配公募高净值交易意图")
                        .fallback(true)
                        .build();
            }

            @Override
            public String generateSummaryReply(String query, List<String> matchedAssetNames) {
                if (matchedAssetNames == null || matchedAssetNames.isEmpty()) {
                    return "未检索到完全匹配的资产";
                }
                return "已为您找到相关资产: " + String.join(", ", matchedAssetNames);
            }
        };

        AskMetadataLogGateway logGateway = (id, userId, queryText, matchedAssetIds, confidenceScore, modelName, createdAt) -> {

            loggedQueries.add(queryText);
        };

        service = new AskMetadataApplicationService(llmGateway, searchIndex, logGateway);
    }

    @Test
    @DisplayName("自然语言找数：匹配业务术语并召回正确资产")
    void testAskMetadataMatchKeywords() {
        AskMetadataSession session = service.askMetadata("帮我查找公募高净值交易流水", "trade", 5, "user-001");

        assertThat(session).isNotNull();
        assertThat(session.isFallback()).isTrue();
        assertThat(session.getMatchedAssets()).isNotEmpty();
        assertThat(session.getMatchedAssets().get(0).getAssetName()).isEqualTo("dwd_trade_order_di");
        assertThat(session.getMatchedAssets().get(0).getHealthScore()).isGreaterThanOrEqualTo(90);
        assertThat(loggedQueries).contains("帮我查找公募高净值交易流水");
    }

    @Test
    @DisplayName("自然语言找数：空意图或未命中返回友好回复")
    void testAskMetadataEmptyResult() {
        AskMetadataSession session = service.askMetadata("完全不存在的生僻资产名称xyz", null, 5, "user-001");

        assertThat(session).isNotNull();
        assertThat(session.getReply()).contains("未检索到完全匹配的资产");
    }
}
