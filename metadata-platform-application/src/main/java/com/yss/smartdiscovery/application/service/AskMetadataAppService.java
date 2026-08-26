package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.application.dto.AskResponseDTO;
import com.yss.smartdiscovery.application.dto.RecommendedAssetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AskMetadataAppService {

    public AskResponseDTO askMetadata(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return AskResponseDTO.builder()
                    .intentSummary("请输入有效找数需求")
                    .extractedEntities(Collections.emptyList())
                    .extractedTerms(Collections.emptyList())
                    .recommendedAssets(Collections.emptyList())
                    .build();
        }

        List<String> entities = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        List<RecommendedAssetDTO> assets = new ArrayList<>();

        if (queryText.contains("华东") || queryText.contains("高净值") || queryText.contains("交易")) {
            entities.add("客户 (Customer)");
            entities.add("交易 (Trade)");
            terms.add("高净值交易额 (Certified)");

            assets.add(RecommendedAssetDTO.builder()
                    .tableName("ads_vip_trade_east_china_di")
                    .tableCnName("华东区高净值客户交易聚合日表")
                    .matchScore(98)
                    .recommendReason("精准命中地区「华东」、客户分层「高净值 (cust_level >= 4)」、指标「交易额 (trans_amount)」，并由数仓 ADS 层每日产出")
                    .certifiedTerm("高净值交易口径 v1.2")
                    .dqHealthScore(96)
                    .dqLevel("优")
                    .build());

            assets.add(RecommendedAssetDTO.builder()
                    .tableName("dwd_trade_order_di")
                    .tableCnName("全行交易订单明细事实表")
                    .matchScore(91)
                    .recommendReason("包含订单原始交易金额、省市代码 (province_code=310000 华东) 与客户 ID 字段，支持底层明细下钻")
                    .certifiedTerm("交易订单规范 v2.0")
                    .dqHealthScore(94)
                    .dqLevel("优")
                    .build());
        } else {
            entities.add("通用数据资产");
            terms.add("基础业务表");
            assets.add(RecommendedAssetDTO.builder()
                    .tableName("dwd_trade_order_di")
                    .tableCnName("全行交易订单明细事实表")
                    .matchScore(85)
                    .recommendReason("匹配基础交易事实表")
                    .certifiedTerm("交易规范 v1.0")
                    .dqHealthScore(90)
                    .dqLevel("优")
                    .build());
        }

        return AskResponseDTO.builder()
                .intentSummary("已提取实体「" + String.join("、", entities) + "」，关联口径「" + String.join("、", terms) + "」")
                .extractedEntities(entities)
                .extractedTerms(terms)
                .recommendedAssets(assets)
                .build();
    }

    public List<String> getSuggestions() {
        return Arrays.asList(
                "查找近半年华东地区高净值客户交易明细",
                "有哪些核心表包含手机号和身份证且未脱敏？",
                "查找用于每日对账的财务核心结算表及健康分"
        );
    }
}
