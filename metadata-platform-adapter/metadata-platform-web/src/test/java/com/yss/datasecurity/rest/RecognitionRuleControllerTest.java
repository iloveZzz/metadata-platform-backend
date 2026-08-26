package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RecognitionRuleCreateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleManualScanDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestResultVO;
import com.yss.datasecurity.application.dto.RecognitionRuleTransferOwnerDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleVO;
import com.yss.datasecurity.application.service.RecognitionRuleAppService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecognitionRuleController.class)
@ContextConfiguration(classes = {RecognitionRuleController.class, DataSecurityExceptionAdvice.class})
class RecognitionRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecognitionRuleAppService recognitionRuleAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/sec/recognition-rules - 分页查询识别规则")
    void testPageRules() throws Exception {
        RecognitionRuleVO vo = RecognitionRuleVO.builder()
                .id(2001L)
                .ruleName("客户身份识别")
                .description("全库识别客户身份证、手机号与姓名")
                .categoryScopeMode("ALL")
                .scanSourceType("COMPUTE_ENGINE")
                .owner("admin")
                .status("ENABLED")
                .taggedFieldsCount(1420)
                .build();

        PageResult<RecognitionRuleVO> pageResult = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(recognitionRuleAppService.pageRules(anyInt(), anyInt(), any(), any(), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/sec/recognition-rules?pageIndex=1&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ruleName").value("客户身份识别"))
                .andExpect(jsonPath("$.data[0].taggedFieldsCount").value(1420));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-rules - 创建识别规则")
    void testCreateRule() throws Exception {
        RecognitionRuleCreateDTO dto = RecognitionRuleCreateDTO.builder()
                .ruleName("财务扫描规则")
                .description("核心财务库扫描")
                .categoryScopeMode("ALL")
                .scanSourceType("DATASOURCE")
                .priority(20)
                .owner("admin")
                .build();

        when(recognitionRuleAppService.create(any(RecognitionRuleCreateDTO.class))).thenReturn(2002L);

        mockMvc.perform(post("/api/v1/sec/recognition-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(2002L));
    }

    @Test
    @DisplayName("PUT /api/v1/sec/recognition-rules/{id}/status - 切换启停状态")
    void testToggleStatus() throws Exception {
        mockMvc.perform(put("/api/v1/sec/recognition-rules/2001/status?status=DISABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-rules/{id}/clone - 克隆规则")
    void testCloneRule() throws Exception {
        when(recognitionRuleAppService.cloneRule(2001L)).thenReturn(2003L);

        mockMvc.perform(post("/api/v1/sec/recognition-rules/2001/clone"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(2003L));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-rules/transfer-owner - 转交负责人")
    void testTransferOwner() throws Exception {
        RecognitionRuleTransferOwnerDTO dto = RecognitionRuleTransferOwnerDTO.builder()
                .ruleIds(java.util.Arrays.asList(2001L, 2002L))
                .newOwner("sec_manager")
                .build();

        mockMvc.perform(post("/api/v1/sec/recognition-rules/transfer-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-rules/manual-scan - 手动规则扫描触发")
    void testManualScan() throws Exception {
        RecognitionRuleManualScanDTO dto = RecognitionRuleManualScanDTO.builder()
                .scanScopeType("ALL_DB")
                .ruleScope("ENABLED_ONLY")
                .build();

        when(recognitionRuleAppService.manualScan(any(RecognitionRuleManualScanDTO.class))).thenReturn(5);

        mockMvc.perform(post("/api/v1/sec/recognition-rules/manual-scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-rules/test - 抽样规则测试")
    void testTestRule() throws Exception {
        RecognitionRuleTestDTO dto = RecognitionRuleTestDTO.builder()
                .ruleId(2001L)
                .testScopeType("TABLE")
                .targetIdentifiers(java.util.Collections.singletonList("t_user"))
                .build();

        RecognitionRuleTestResultVO match = RecognitionRuleTestResultVO.builder()
                .projectOrDatasource("default")
                .tableName("t_user")
                .columnName("id_card_no")
                .columnComment("身份证号")
                .dataType("varchar(32)")
                .sampleValue("110101199003072345")
                .matchedCategory("居民身份证")
                .matchedGrade("L4")
                .confidence(0.98)
                .matchedRule("客户身份识别")
                .build();

        when(recognitionRuleAppService.testRule(any(RecognitionRuleTestDTO.class))).thenReturn(java.util.Collections.singletonList(match));

        mockMvc.perform(post("/api/v1/sec/recognition-rules/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].columnName").value("id_card_no"))
                .andExpect(jsonPath("$.data[0].matchedCategory").value("居民身份证"));
    }
}
