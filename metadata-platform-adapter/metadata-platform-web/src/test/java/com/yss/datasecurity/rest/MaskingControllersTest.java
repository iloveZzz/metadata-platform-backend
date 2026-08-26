package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.MaskEvaluationResponseVO;
import com.yss.datasecurity.application.dto.MaskQueryEvaluationRequestDTO;
import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleVO;
import com.yss.datasecurity.application.service.MaskingRuleAppService;
import com.yss.datasecurity.rest.advice.DataSecurityExceptionAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MaskingRuleController.class, MaskingEngineController.class})
@ContextConfiguration(classes = {MaskingRuleController.class, MaskingEngineController.class, DataSecurityExceptionAdvice.class})
class MaskingControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaskingRuleAppService maskingRuleAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/masking-rules - 分页查询脱敏规则列表成功")
    void testPageRules() throws Exception {
        MaskingRuleVO vo = MaskingRuleVO.builder()
            .id(8001L)
            .ruleName("手机号遮盖脱敏")
            .algorithmType("MASKING")
            .status("ACTIVE")
            .build();

        PageResult<MaskingRuleVO> pageResult = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(maskingRuleAppService.pageRules(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/masking-rules?pageIndex=1&pageSize=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].ruleName").value("手机号遮盖脱敏"))
            .andExpect(jsonPath("$.data[0].algorithmType").value("MASKING"));
    }

    @Test
    @DisplayName("POST /api/v1/masking-rules - 创建脱敏规则返回 201 Created")
    void testCreateRule() throws Exception {
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

        when(maskingRuleAppService.createRule(any(MaskingRuleCreateDTO.class))).thenReturn(8001L);

        mockMvc.perform(post("/api/v1/masking-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(8001L));
    }

    @Test
    @DisplayName("POST /api/v1/masking-engine/mask-query - 动态脱敏评估执行成功")
    void testEvaluateMaskQuery() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("phone", "13812345678");

        MaskQueryEvaluationRequestDTO request = MaskQueryEvaluationRequestDTO.builder()
            .datasourceId("ds_prod")
            .tableName("t_user")
            .rawRows(Collections.singletonList(row))
            .build();

        Map<String, Object> maskedRow = new HashMap<>();
        maskedRow.put("phone", "138****5678");

        MaskEvaluationResponseVO responseVO = MaskEvaluationResponseVO.builder()
            .whitelisted(false)
            .appliedRulesCount(1)
            .maskedRows(Collections.singletonList(maskedRow))
            .build();

        when(maskingRuleAppService.evaluateMaskQuery(any(MaskQueryEvaluationRequestDTO.class))).thenReturn(responseVO);

        mockMvc.perform(post("/api/v1/masking-engine/mask-query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedRows[0].phone").value("138****5678"))
            .andExpect(jsonPath("$.data.appliedRulesCount").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/masking-rules/{id}/status - 切换规则状态返回 200")
    void testUpdateStatus() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/masking-rules/8001/status?status=DISABLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/masking-rules/transfer - 批量转交负责人返回 200")
    void testTransferOwner() throws Exception {
        com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO dto =
            com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO.builder()
                .ruleIds(Collections.singletonList(8001L))
                .newOwner("sec_admin_02")
                .build();

        mockMvc.perform(post("/api/v1/masking-rules/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("GET & PUT /api/v1/masking-rules/default-policy - 默认脱敏策略查询与更新")
    void testDefaultPolicy() throws Exception {
        com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO vo =
            com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO.builder()
                .id(1L)
                .securityGrade("L3")
                .algorithmType("MASK_FIXED_STAR")
                .build();

        when(maskingRuleAppService.getDefaultPolicy()).thenReturn(vo);

        mockMvc.perform(get("/api/v1/masking-rules/default-policy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.securityGrade").value("L3"));

        com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO dto =
            com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO.builder()
                .securityGrade("L4")
                .algorithmType("NULL_VALUE")
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/masking-rules/default-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }
}
