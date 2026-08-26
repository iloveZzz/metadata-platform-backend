package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.datasecurity.application.dto.MaskingWhitelistCreateDTO;
import com.yss.datasecurity.application.service.MaskingWhitelistAppService;
import com.yss.datasecurity.rest.advice.DataSecurityExceptionAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaskingWhitelistController.class)
@ContextConfiguration(classes = {MaskingWhitelistController.class, DataSecurityExceptionAdvice.class})
class WhitelistAndAuditControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaskingWhitelistAppService whitelistAppService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("POST /api/v1/masking-whitelists - 申请创建时效白名单")
    void testCreateWhitelist() throws Exception {
        when(whitelistAppService.createWhitelist(any(MaskingWhitelistCreateDTO.class))).thenReturn(1001L);

        MaskingWhitelistCreateDTO dto = MaskingWhitelistCreateDTO.builder()
                .granteeType("USER")
                .granteeId("analyst_01")
                .categoryId(10L)
                .ruleId(20L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .reason("临时排障")
                .build();

        mockMvc.perform(post("/api/v1/masking-whitelists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1001));
    }

    @Test
    @DisplayName("POST /api/v1/masking-whitelists/{id}/revoke - 提前撤销白名单")
    void testRevokeWhitelist() throws Exception {
        mockMvc.perform(post("/api/v1/masking-whitelists/1001/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
