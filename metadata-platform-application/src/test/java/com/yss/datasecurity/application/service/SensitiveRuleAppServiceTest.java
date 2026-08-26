package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.SensitiveRuleConvertor;
import com.yss.datasecurity.application.dto.RuleSimulationRequestDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;
import com.yss.datasecurity.application.service.impl.SensitiveRuleAppServiceImpl;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.SensitiveRuleGateway;
import com.yss.datasecurity.domain.model.SensitiveRule;
import com.yss.datasecurity.domain.service.SensitiveRecognitionSimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveRuleAppServiceTest {

    @Mock
    private SensitiveRuleGateway sensitiveRuleGateway;

    private SensitiveRuleAppService sensitiveRuleAppService;

    @BeforeEach
    void setUp() {
        sensitiveRuleAppService = new SensitiveRuleAppServiceImpl(
            sensitiveRuleGateway,
            new SensitiveRecognitionSimulationEngine(),
            org.mapstruct.factory.Mappers.getMapper(SensitiveRuleConvertor.class)
        );
    }

    @Test
    @DisplayName("测试创建敏感识别规则 - 成功创建并校验优先级")
    void testCreateRule_Success() {
        SensitiveRuleCreateDTO dto = SensitiveRuleCreateDTO.builder()
            .ruleName("客户手机号智能识别")
            .priority(20)
            .categoryScopeMode("ALL")
            .scanScopeType("DATASOURCE")
            .description("识别用户手机号码")
            .build();

        when(sensitiveRuleGateway.findByName("客户手机号智能识别")).thenReturn(Optional.empty());
        when(sensitiveRuleGateway.save(any(SensitiveRule.class))).thenAnswer(invocation -> {
            SensitiveRule r = invocation.getArgument(0);
            r.setId(4001L);
            return r;
        });

        Long id = sensitiveRuleAppService.create(dto);
        assertNotNull(id);
        assertEquals(4001L, id);
    }

    @Test
    @DisplayName("测试克隆敏感识别特征 - 自动生成 _COPY 后缀且类型为 CUSTOM")
    void testCloneRule_Success() {
        Long sourceId = 4001L;
        SensitiveRule source = SensitiveRule.builder()
            .id(sourceId)
            .ruleName("银行卡号识别")
            .ruleType("BUILTIN")
            .priority(10)
            .owner("admin")
            .status("ENABLED")
            .categoryScopeMode("ALL")
            .scanScopeType("DATASOURCE")
            .taggedFieldsCount(120)
            .build();

        when(sensitiveRuleGateway.findById(sourceId)).thenReturn(Optional.of(source));
        when(sensitiveRuleGateway.save(any(SensitiveRule.class))).thenAnswer(invocation -> {
            SensitiveRule clone = invocation.getArgument(0);
            assertEquals("银行卡号识别_COPY", clone.getRuleName());
            assertEquals("CUSTOM", clone.getRuleType());
            assertEquals("ENABLED", clone.getStatus());
            assertEquals(0, clone.getTaggedFieldsCount());
            clone.setId(4002L);
            return clone;
        });

        Long newId = sensitiveRuleAppService.cloneRule(sourceId);
        assertEquals(4002L, newId);
    }

    @Test
    @DisplayName("测试删除内置识别特征 - 触发保护异常")
    void testDeleteBuiltinRule_ThrowsException() {
        Long ruleId = 1001L;
        SensitiveRule builtin = SensitiveRule.builder().id(ruleId).ruleName("性别").ruleType("BUILTIN").build();
        when(sensitiveRuleGateway.findById(ruleId)).thenReturn(Optional.of(builtin));

        DataSecurityException ex = assertThrows(DataSecurityException.class, () -> sensitiveRuleAppService.delete(ruleId));
        assertEquals("BUILTIN_RULE_CANNOT_DELETE", ex.getErrorCode());
    }

    @Test
    @DisplayName("测试重置敏感识别规则 - 清空已识别打标数量")
    void testResetRule_Success() {
        Long ruleId = 4001L;
        SensitiveRule existing = SensitiveRule.builder().id(ruleId).ruleName("测试规则").build();
        when(sensitiveRuleGateway.findById(ruleId)).thenReturn(Optional.of(existing));

        sensitiveRuleAppService.resetRule(ruleId);
        verify(sensitiveRuleGateway).clearTaggedFields(ruleId);
    }

    @Test
    @DisplayName("测试在线模拟采样测试 - 模拟指定数据表返回拟命中字段且不写入数据库")
    void testSimulate_Success() {
        SensitiveRuleCreateDTO draft = SensitiveRuleCreateDTO.builder()
            .ruleName("草稿规则")
            .priority(50)
            .categoryScopeMode("ALL")
            .scanScopeType("DATASOURCE")
            .featureConfig("{\"fieldNameRegex\":\"phone|mobile\"}")
            .build();

        RuleSimulationRequestDTO req = RuleSimulationRequestDTO.builder()
            .datasourceId("ds_mysql_prod")
            .tableNames(Arrays.asList("t_user_info", "t_order"))
            .ruleDraftConfig(draft)
            .build();

        List<SimulationFieldMatchVO> matches = sensitiveRuleAppService.simulate(req);
        assertNotNull(matches);
        assertTrue(matches.size() > 0);
        assertTrue(matches.stream().anyMatch(m -> m.getFieldName().contains("phone") || m.getFieldName().contains("mobile")));
    }
}
