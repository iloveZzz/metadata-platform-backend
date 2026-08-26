package com.yss.datamiddle.smartgovernance;

import com.yss.datamiddle.smartgovernance.domain.llm.LlmGateway;
import com.yss.datamiddle.smartgovernance.domain.llm.LlmInferenceResult;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;
import com.yss.datamiddle.smartgovernance.domain.security.model.FunnelLayer;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;
import com.yss.datamiddle.smartgovernance.domain.security.service.ThreeLayerFunnelScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ThreeLayerFunnelScannerTest {

    private LlmGateway llmGateway;
    private ThreeLayerFunnelScanner scanner;
    private SecurityTemplate template;

    @BeforeEach
    void setUp() {
        llmGateway = Mockito.mock(LlmGateway.class);
        scanner = new ThreeLayerFunnelScanner(llmGateway);

        ClassificationRule phoneRule = ClassificationRule.builder()
                .id("rule-phone")
                .sensitiveType("PHONE")
                .sensitiveName("手机号码")
                .securityLevel(SecurityLevel.L3)
                .regexPattern(".*(phone|mobile|sjhm|shouji).*")
                .dictionaryWords("手机号,联系电话")
                .isActive(true)
                .build();

        template = SecurityTemplate.builder()
                .id("tpl-jr-0197")
                .templateCode("JR_T_0197_2020")
                .templateName("金融数据安全分级指南")
                .defaultAutoApproval(true)
                .defaultThreshold(new BigDecimal("0.95"))
                .rules(Collections.singletonList(phoneRule))
                .build();
    }

    @Test
    @DisplayName("L1 正则完全命中时应直接产出 L1_REGEX 候选并自动采纳")
    void testL1RegexMatch() {
        SensitiveCandidate candidate = scanner.scanColumn(
                template,
                "ds-mysql",
                "trade_db",
                "user_t",
                "用户表",
                "mobile_phone",
                "用户注册手机号",
                "VARCHAR(16)",
                Collections.emptyList()
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.getFunnelLayer()).isEqualTo(FunnelLayer.L1_REGEX);
        assertThat(candidate.getRecommendedLevel()).isEqualTo(SecurityLevel.L3);
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(candidate.getOperator()).isEqualTo("SYSTEM_AUTO_LLM");
    }

    @Test
    @DisplayName("L1/L2 未命中时应触发 L3 大模型上下文推理")
    void testL3LlmInferenceTriggered() {
        when(llmGateway.inferSecurityClassification(any())).thenReturn(
                LlmInferenceResult.builder()
                        .sensitiveType("IDENTIFICATION_CARD")
                        .securityLevel(SecurityLevel.L4)
                        .clauseRef("JR/T 0197 5.2.4")
                        .reasoning("根据注释推断为身份证件号码")
                        .confidence(new BigDecimal("0.92"))
                        .build()
        );

        SensitiveCandidate candidate = scanner.scanColumn(
                template,
                "ds-mysql",
                "trade_db",
                "cust_info_t",
                "客户基本信息表",
                "kh_sfz_no",
                "客户身份认证主键",
                "VARCHAR(32)",
                Collections.emptyList()
        );

        assertThat(candidate).isNotNull();
        assertThat(candidate.getFunnelLayer()).isEqualTo(FunnelLayer.L3_LLM);
        assertThat(candidate.getSensitiveType()).isEqualTo("IDENTIFICATION_CARD");
        assertThat(candidate.getRecommendedLevel()).isEqualTo(SecurityLevel.L4);
        assertThat(candidate.getReasoning()).contains("根据注释推断");
        // 置信度 0.92 < 0.95，因此处于 PENDING 待人工审核状态
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING);
    }
}
