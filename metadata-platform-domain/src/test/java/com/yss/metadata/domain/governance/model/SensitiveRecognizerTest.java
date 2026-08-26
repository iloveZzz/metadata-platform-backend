package com.yss.metadata.domain.governance.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感分类识别引擎测试（WU-04-02 候选自动识别，TDD 红→绿）。
 *
 * <p>覆盖：内置规则（手机号/身份证/银行卡/邮箱按列名/注释关键字）、
 * 自定义规则（正则/列名/字典）、禁用规则跳过、非法正则不阻断、
 * 0 命中空列表（空结构非错误）、命中取首个（内置优先）。</p>
 */
class SensitiveRecognizerTest {

    @Test
    @DisplayName("内置手机号规则：列名含 phone/mobile/手机号 命中 → 敏感-PII/PII")
    void builtinPhoneHit() {
        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("mobile_no", "客户手机号"), Collections.emptyList());

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感-PII");
        assertThat(hits.get(0).getLevel()).isEqualTo("PII");
    }

    @Test
    @DisplayName("内置身份证规则：注释含 身份证 命中（列名不命中时走注释）")
    void builtinIdCardViaComment() {
        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("c1", "用户身份证号码"), Collections.emptyList());

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感-PII");
    }

    @Test
    @DisplayName("内置银行卡/邮箱规则：列名关键字命中")
    void builtinBankCardAndEmail() {
        assertThat(SensitiveRecognizer.recognize(column("bank_card_no", null), Collections.emptyList()))
                .extracting(RecognizedClassification::getName).containsExactly("敏感-PII");
        assertThat(SensitiveRecognizer.recognize(column("user_email", "邮件地址"), Collections.emptyList()))
                .extracting(RecognizedClassification::getName).containsExactly("敏感-PII");
    }

    @Test
    @DisplayName("内置规则优先：内置命中返回，不再检查自定义规则")
    void builtinTakesPriorityOverCustom() {
        ClassRule regex = rule("r1", ClassRuleType.REGEX, "^order_.*", true);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("phone", "订单号"), Collections.singletonList(regex));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感-PII");
    }

    @Test
    @DisplayName("自定义正则规则命中 → 敏感/SENSITIVE（大小写不敏感）")
    void customRegexHit() {
        ClassRule regex = rule("r1", ClassRuleType.REGEX, "^order_.*", true);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("Order_Amount", "交易金额"), Collections.singletonList(regex));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感");
        assertThat(hits.get(0).getLevel()).isEqualTo("SENSITIVE");
    }

    @Test
    @DisplayName("自定义列名规则：列名包含关键字命中")
    void customColumnRuleHit() {
        ClassRule column = rule("r2", ClassRuleType.COLUMN, "salary", true);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("monthly_salary", null), Collections.singletonList(column));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感");
    }

    @Test
    @DisplayName("自定义字典规则：逗号分隔关键字任一命中（列名或注释）")
    void customDictionaryRuleHit() {
        ClassRule dictionary = rule("r3", ClassRuleType.DICTIONARY, "工号,绩效,考勤", true);

        List<RecognizedClassification> viaComment = SensitiveRecognizer.recognize(column("f1", "员工绩效数据"), Collections.singletonList(dictionary));
        assertThat(viaComment).hasSize(1);

        List<RecognizedClassification> viaName = SensitiveRecognizer.recognize(column("emp_no", "工号"), Collections.singletonList(dictionary));
        assertThat(viaName).hasSize(1);
    }

    @Test
    @DisplayName("禁用规则跳过：命中不产出候选（内置仍生效）")
    void disabledRuleSkipped() {
        ClassRule disabledRegex = rule("r1", ClassRuleType.REGEX, "^order_.*", false);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("order_id", "订单号"), Collections.singletonList(disabledRegex));

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("非法正则不阻断：不抛异常且不命中")
    void invalidRegexDoesNotBlock() {
        ClassRule badRegex = rule("r1", ClassRuleType.REGEX, "[unclosed", true);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("c1", "普通列"), Collections.singletonList(badRegex));

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("0 命中返回空列表（空结构非错误）")
    void noHitReturnsEmpty() {
        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(column("created_at", "创建时间"), Collections.emptyList());

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("多自定义规则按序取首个命中")
    void firstHitWinsByRuleOrder() {
        ClassRule first = rule("r1", ClassRuleType.REGEX, ".*secret.*", true);
        ClassRule second = rule("r2", ClassRuleType.COLUMN, "note", true);

        List<RecognizedClassification> hits = SensitiveRecognizer.recognize(
                column("secret_note", null), Arrays.asList(first, second));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getName()).isEqualTo("敏感");
    }

    private RecognizableColumn column(String name, String comment) {
        return RecognizableColumn.builder().assetId("a-1").columnId("c-1")
                .name(name).comment(comment).build();
    }

    private ClassRule rule(String id, ClassRuleType type, String pattern, boolean enabled) {
        return ClassRule.builder().id(id).name("规则-" + id).type(type)
                .pattern(pattern).enabled(enabled).build();
    }
}
