package com.yss.smartdiscovery.domain;

import com.yss.smartdiscovery.domain.rule.SandboxTester;
import com.yss.smartdiscovery.domain.rule.TagRule;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTaxonomyTest {

    @Test
    @DisplayName("创建标签定义 - 校验必填项")
    void testCreateTagValidation() {
        SmartTagDefinition tag = SmartTagDefinition.builder()
                .tagName("测试标签")
                .tagCode("TEST_TAG")
                .categoryCode("DOMAIN")
                .build();
        tag.validate();
        assertThat(tag.getTagName()).isEqualTo("测试标签");

        SmartTagDefinition invalidTag = SmartTagDefinition.builder()
                .tagName("")
                .tagCode("TEST_TAG")
                .build();
        assertThatThrownBy(invalidTag::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标签名称不能为空");
    }

    @Test
    @DisplayName("Layer 1 正则规则语法校验与匹配")
    void testRegexRuleValidationAndMatch() {
        TagRule validRule = TagRule.builder()
                .regexPattern("^(cust_id|id_card|identity_no)")
                .build();
        validRule.validate();
        assertThat(validRule.matchesRegex("cust_id_card", "身份证号")).isTrue();
        assertThat(validRule.matchesRegex("create_time", "创建时间")).isFalse();

        TagRule invalidRule = TagRule.builder()
                .regexPattern("[a-z")
                .build();
        assertThatThrownBy(invalidRule::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("正则表达式语法非法");
    }

    @Test
    @DisplayName("在线三层漏斗规则沙箱测试")
    void testSandboxTester() {
        SmartTagDefinition secTag = SmartTagDefinition.builder()
                .id("TAG-01")
                .tagName("L4 核心敏感数据")
                .tagCode("SEC_L4")
                .categoryCode("SECURITY")
                .tagRule(TagRule.builder()
                        .regexPattern("^(cust_id|id_card|identity_no)")
                        .boundTermNames(Arrays.asList("身份证", "银行卡"))
                        .build())
                .build();

        // 1. 测试正则命中
        SandboxTester.SandboxResult res1 = SandboxTester.testField("cust_id_card", "证件号", Collections.singletonList(secTag));
        assertThat(res1.getL1RegexHit()).isTrue();
        assertThat(res1.getConfidence()).isEqualTo(0.98);
        assertThat(res1.getMatchedTagName()).isEqualTo("L4 核心敏感数据");

        // 2. 测试词库命中
        SandboxTester.SandboxResult res2 = SandboxTester.testField("user_cert_code", "包含身份证信息", Collections.singletonList(secTag));
        assertThat(res2.getL2GlossaryHit()).isTrue();
        assertThat(res2.getConfidence()).isEqualTo(0.88);
    }
}
