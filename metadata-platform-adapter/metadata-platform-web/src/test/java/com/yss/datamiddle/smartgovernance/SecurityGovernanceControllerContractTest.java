package com.yss.datamiddle.smartgovernance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.smartgovernance.application.service.SecurityGovernanceApplicationService;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.FunnelLayer;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;
import com.yss.datamiddle.smartgovernance.web.controller.SecurityGovernanceController;
import com.yss.datamiddle.smartgovernance.web.dto.BatchApproveCandidatesDTO;
import com.yss.datamiddle.smartgovernance.web.dto.CreateSecurityTemplateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityGovernanceController.class)
@ContextConfiguration(classes = {SecurityGovernanceController.class})
class SecurityGovernanceControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityGovernanceApplicationService securityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/smart-governance/security/templates 返回模板列表")
    void testListTemplates() throws Exception {
        SecurityTemplate tpl = SecurityTemplate.builder()
                .id("tpl-jr-0197")
                .templateCode("JR_T_0197_2020")
                .templateName("金融数据安全分级指南")
                .defaultAutoApproval(true)
                .build();
        when(securityService.listTemplates(any())).thenReturn(Collections.singletonList(tpl));

        mockMvc.perform(get("/api/smart-governance/security/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].templateCode").value("JR_T_0197_2020"));
    }

    @Test
    @DisplayName("POST /api/smart-governance/security/templates 创建模板返回 200")
    void testCreateTemplate() throws Exception {
        when(securityService.createTemplate(any(), any())).thenReturn("tpl-new-001");

        CreateSecurityTemplateDTO dto = new CreateSecurityTemplateDTO();
        dto.setTemplateCode("CUSTOM_PIPL");
        dto.setTemplateName("企业自研个人隐私模板");

        mockMvc.perform(post("/api/smart-governance/security/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("tpl-new-001"));
    }

    @Test
    @DisplayName("GET /api/smart-governance/security/candidates 返回 PageResult 结构")
    void testListCandidates() throws Exception {
        SensitiveCandidate candidate = SensitiveCandidate.builder()
                .id("cdd-001")
                .tableName("cust_info_t")
                .columnName("kh_sfz_no")
                .recommendedLevel(SecurityLevel.L4)
                .confidence(new BigDecimal("0.92"))
                .funnelLayer(FunnelLayer.L3_LLM)
                .status(CandidateStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(securityService.queryCandidates(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(candidate));
        when(securityService.countCandidates(any(), any(), any(), any())).thenReturn(1L);

        mockMvc.perform(get("/api/smart-governance/security/candidates?pageIndex=1&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].columnName").value("kh_sfz_no"))
                .andExpect(jsonPath("$.data[0].recommendedLevel").value("L4"));
    }

    @Test
    @DisplayName("POST /api/smart-governance/security/candidates/batch-approve 批量采纳")
    void testBatchApprove() throws Exception {
        Map<String, Integer> res = new HashMap<>();
        res.put("successCount", 2);
        res.put("failureCount", 0);
        when(securityService.batchApprove(any(), any())).thenReturn(res);

        BatchApproveCandidatesDTO dto = new BatchApproveCandidatesDTO();
        dto.setCandidateIds(Collections.singletonList("cdd-001"));

        mockMvc.perform(post("/api/smart-governance/security/candidates/batch-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2));
    }
}
