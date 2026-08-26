package com.yss.datamiddle.smartgovernance;

import com.yss.datamiddle.smartgovernance.domain.llm.PromptPayload;
import com.yss.datamiddle.smartgovernance.domain.security.service.ZeroDataLeakAssertion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZeroDataLeakAssertionTest {

    @Test
    @DisplayName("纯元数据 Schema 白名单应通过断言校验")
    void testSafePayloadPasses() {
        PromptPayload payload = PromptPayload.builder()
                .databaseName("trade_db")
                .tableName("cust_info_t")
                .tableComment("客户基本信息表")
                .columnName("kh_sfz_no")
                .columnComment("客户身份认证主键")
                .dataType("VARCHAR(32)")
                .neighborColumnNames(Collections.singletonList("cust_name"))
                .standardTemplateCode("JR_T_0197_2020")
                .build();

        assertThatCode(() -> ZeroDataLeakAssertion.assertSafePayload(payload))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("当 Prompt 中注入疑似真实手机号或身份证数据行时应触发安全拦截")
    void testRealDataThrowsSecurityException() {
        PromptPayload leakedPayload = PromptPayload.builder()
                .databaseName("trade_db")
                .tableName("cust_info_t")
                .tableComment("包含真实样本: 13812345678") // 泄露真实手机号
                .columnName("mobile")
                .dataType("VARCHAR(11)")
                .build();

        assertThatThrownBy(() -> ZeroDataLeakAssertion.assertSafePayload(leakedPayload))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("CRITICAL SAFETY VIOLATION");
    }
}
