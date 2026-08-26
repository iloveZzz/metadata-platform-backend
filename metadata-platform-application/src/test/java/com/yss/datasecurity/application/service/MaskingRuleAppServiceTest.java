package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.MaskingRuleConvertor;
import com.yss.datasecurity.application.dto.MaskEvaluationResponseVO;
import com.yss.datasecurity.application.dto.MaskQueryEvaluationRequestDTO;
import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.service.impl.MaskingRuleAppServiceImpl;
import com.yss.datasecurity.domain.gateway.MaskingRuleGateway;
import com.yss.datasecurity.domain.model.MaskingRule;
import com.yss.datasecurity.domain.service.MaskingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaskingRuleAppServiceTest {

    @Mock
    private MaskingRuleGateway maskingRuleGateway;

    private MaskingRuleAppService maskingRuleAppService;

    @BeforeEach
    void setUp() {
        maskingRuleAppService = new MaskingRuleAppServiceImpl(
            maskingRuleGateway,
            new MaskingEngine(),
            org.mapstruct.factory.Mappers.getMapper(MaskingRuleConvertor.class)
        );
    }

    @Test
    @DisplayName("测试创建脱敏规则")
    void testCreateRule() {
        Map<String, Object> params = new HashMap<>();
        params.put("start", 3);
        params.put("end", 7);
        params.put("maskChar", "*");

        MaskingRuleCreateDTO dto = MaskingRuleCreateDTO.builder()
            .ruleName("手机号遮盖脱敏")
            .categoryId(2001L)
            .algorithmType("MASKING")
            .algorithmParams(params)
            .build();

        when(maskingRuleGateway.save(any(MaskingRule.class))).thenAnswer(invocation -> {
            MaskingRule r = invocation.getArgument(0);
            r.setId(8001L);
            return r;
        });

        Long ruleId = maskingRuleAppService.createRule(dto);
        assertEquals(8001L, ruleId);
    }

    @Test
    @DisplayName("测试脱敏计算引擎 - 命中特定遮盖规则与 FPE 保留格式加密")
    void testEvaluateMaskQuery_HitRule() {
        Map<String, Object> maskParams = new HashMap<>();
        maskParams.put("start", 3);
        maskParams.put("end", 7);
        maskParams.put("maskChar", "*");

        MaskingRule rulePhone = MaskingRule.builder()
            .id(8001L)
            .ruleName("phone")
            .algorithmType("MASKING")
            .algorithmParams(maskParams)
            .status("ACTIVE")
            .build();

        when(maskingRuleGateway.listActiveRules()).thenReturn(Collections.singletonList(rulePhone));

        Map<String, Object> row = new HashMap<>();
        row.put("user_name", "张三");
        row.put("user_phone", "13812345678");

        MaskQueryEvaluationRequestDTO request = MaskQueryEvaluationRequestDTO.builder()
            .datasourceId("ds_prod")
            .tableName("t_user")
            .rawRows(Collections.singletonList(row))
            .build();

        MaskEvaluationResponseVO response = maskingRuleAppService.evaluateMaskQuery(request);
        assertNotNull(response);
        assertEquals(1, response.getMaskedRows().size());
        Map<String, Object> masked = response.getMaskedRows().get(0);
        assertEquals("张三", masked.get("user_name"));
        assertEquals("138****5678", masked.get("user_phone"));
    }

    @Test
    @DisplayName("测试脱敏计算引擎 - 未配置专属规则的敏感字段自动触发 L3+ 默认托底遮盖")
    void testEvaluateMaskQuery_DefaultFallback() {
        when(maskingRuleGateway.listActiveRules()).thenReturn(Collections.emptyList());

        Map<String, Object> row = new HashMap<>();
        row.put("id_card", "110101199003072345"); // 未配规则，但命中敏感命名字段
        row.put("normal_col", "public_data");

        MaskQueryEvaluationRequestDTO request = MaskQueryEvaluationRequestDTO.builder()
            .datasourceId("ds_prod")
            .tableName("t_user")
            .rawRows(Collections.singletonList(row))
            .build();

        MaskEvaluationResponseVO response = maskingRuleAppService.evaluateMaskQuery(request);
        Map<String, Object> masked = response.getMaskedRows().get(0);
        assertEquals("public_data", masked.get("normal_col"));
        String maskedIdCard = String.valueOf(masked.get("id_card"));
        assertTrue(maskedIdCard.contains("*"));
        assertEquals(18, maskedIdCard.length());
    }

    @Test
    @DisplayName("测试更新生效状态与转交负责人")
    void testUpdateStatusAndTransferOwner() {
        MaskingRule rule = MaskingRule.builder()
            .id(9001L)
            .ruleName("测试规则")
            .status("ENABLED")
            .owner("原负责人")
            .build();

        when(maskingRuleGateway.findById(9001L)).thenReturn(java.util.Optional.of(rule));

        maskingRuleAppService.updateStatus(9001L, "DISABLED");
        assertEquals("DISABLED", rule.getStatus());

        com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO transferDTO =
            com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO.builder()
                .ruleIds(Collections.singletonList(9001L))
                .newOwner("新负责人")
                .build();
        maskingRuleAppService.transferOwner(transferDTO);
        assertEquals("新负责人", rule.getOwner());
    }

    @Test
    @DisplayName("测试默认脱敏策略获取与更新")
    void testDefaultPolicy() {
        com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO current = maskingRuleAppService.getDefaultPolicy();
        assertNotNull(current);

        com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO updateDTO =
            com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO.builder()
                .securityGrade("L4")
                .algorithmType("NULL_VALUE")
                .description("绝密数据置空处理")
                .build();
        maskingRuleAppService.saveDefaultPolicy(updateDTO);

        com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO updated = maskingRuleAppService.getDefaultPolicy();
        assertEquals("L4", updated.getSecurityGrade());
        assertEquals("NULL_VALUE", updated.getAlgorithmType());
    }
}
