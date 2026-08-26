package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RuleSimulationRequestDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleVO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;
import com.yss.datasecurity.application.service.SensitiveRuleAppService;
import com.yss.datasecurity.rest.advice.DataSecurityExceptionAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensitiveRuleController.class)
@ContextConfiguration(classes = {SensitiveRuleController.class, DataSecurityExceptionAdvice.class})
class SensitiveRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SensitiveRuleAppService sensitiveRuleAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/sensitive-rules - 分页查询成功返回规则列表")
    void testPageRules() throws Exception {
        SensitiveRuleVO vo = SensitiveRuleVO.builder()
            .id(4001L)
            .ruleName("手机号识别规则")
            .priority(20)
            .status("ENABLED")
            .scanScopeType("DATASOURCE")
            .taggedFieldsCount(55)
            .build();

        PageResult<SensitiveRuleVO> pageResult = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(sensitiveRuleAppService.pageRules(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/sensitive-rules?pageIndex=1&pageSize=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].ruleName").value("手机号识别规则"))
            .andExpect(jsonPath("$.data[0].taggedFieldsCount").value(55));
    }

    @Test
    @DisplayName("POST /api/v1/sensitive-rules/{id}/clone - 克隆成功返回 201 与新规则 ID")
    void testCloneRule() throws Exception {
        when(sensitiveRuleAppService.cloneRule(4001L)).thenReturn(4002L);

        mockMvc.perform(post("/api/v1/sensitive-rules/4001/clone"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(4002L));
    }

    @Test
    @DisplayName("POST /api/v1/sensitive-rules/simulate - 在线模拟采样测试成功返回匹配预览")
    void testSimulate() throws Exception {
        SensitiveRuleCreateDTO draft = SensitiveRuleCreateDTO.builder()
            .ruleName("测试规则")
            .priority(50)
            .categoryScopeMode("ALL")
            .scanScopeType("DATASOURCE")
            .build();

        RuleSimulationRequestDTO req = RuleSimulationRequestDTO.builder()
            .datasourceId("ds1")
            .tableNames(Collections.singletonList("t_user"))
            .ruleDraftConfig(draft)
            .build();

        SimulationFieldMatchVO match = SimulationFieldMatchVO.builder()
            .tableName("t_user")
            .fieldName("mobile_phone")
            .sampleValue("13800138000")
            .matchedCategoryName("手机号")
            .securityGradeName("L3 敏感机密")
            .matchedCondition("字段名正则命中")
            .build();

        when(sensitiveRuleAppService.simulate(any(RuleSimulationRequestDTO.class))).thenReturn(Collections.singletonList(match));

        mockMvc.perform(post("/api/v1/sensitive-rules/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].fieldName").value("mobile_phone"))
            .andExpect(jsonPath("$.data[0].matchedCategoryName").value("手机号"));
    }
}
