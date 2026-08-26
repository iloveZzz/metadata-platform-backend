package com.yss.smartdiscovery.domain;

import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.rule.TagRule;
import com.yss.smartdiscovery.domain.rule.TaggingFunnelEngine;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaggingFunnelEngineTest {

    @Test
    @DisplayName("三层漏斗分析流水线 - 正则/词库/LLM 与双通道生效")
    void testFunnelAnalysisAndThreshold() {
        SmartTagDefinition l4Tag = SmartTagDefinition.builder()
                .id("TAG-01")
                .tagName("L4 核心敏感数据")
                .categoryCode("SECURITY")
                .tagRule(TagRule.builder()
                        .regexPattern("^(cust_id|id_card|identity_no|mobile)")
                        .boundTermNames(Arrays.asList("身份证", "手机号"))
                        .build())
                .build();

        SmartTagDefinition tradeTag = SmartTagDefinition.builder()
                .id("TAG-02")
                .tagName("零售金融交易域")
                .categoryCode("DOMAIN")
                .tagRule(TagRule.builder()
                        .regexPattern("^(trade_|order_|trans_)")
                        .boundTermNames(Arrays.asList("实际成交额", "交易金额"))
                        .build())
                .build();

        List<TaggingFunnelEngine.ColumnTarget> targets = Arrays.asList(
                TaggingFunnelEngine.ColumnTarget.builder().tableName("t_order").columnName("cust_id_card").columnComment("身份证号").build(),
                TaggingFunnelEngine.ColumnTarget.builder().tableName("t_order").columnName("trans_amt").columnComment("交易金额").build(),
                TaggingFunnelEngine.ColumnTarget.builder().tableName("t_user").columnName("fuzzy_field").columnComment("未知描述").build()
        );

        List<SmartTagCandidate> candidates = TaggingFunnelEngine.analyze(targets, Arrays.asList(l4Tag, tradeTag), 0.90);

        assertThat(candidates).hasSize(3);

        // 1. cust_id_card 命中 L1 正则 -> 高置信 0.98 -> AUTO_APPLIED
        SmartTagCandidate c1 = candidates.get(0);
        assertThat(c1.getSource()).isEqualTo("L1_RULE");
        assertThat(c1.getConfidence()).isEqualTo(0.98);
        assertThat(c1.getStatus()).isEqualTo("AUTO_APPLIED");

        // 2. trans_amt 命中 L1 正则/词库 -> AUTO_APPLIED
        SmartTagCandidate c2 = candidates.get(1);
        assertThat(c2.getConfidence()).isGreaterThanOrEqualTo(0.88);

        // 3. fuzzy_field 兜底 L3 LLM -> 置信度 0.82 < 0.90 -> PENDING
        SmartTagCandidate c3 = candidates.get(2);
        assertThat(c3.getSource()).isEqualTo("L3_LLM");
        assertThat(c3.getStatus()).isEqualTo("PENDING");
    }
}
