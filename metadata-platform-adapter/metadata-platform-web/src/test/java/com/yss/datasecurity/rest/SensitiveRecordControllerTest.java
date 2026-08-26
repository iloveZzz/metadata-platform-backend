package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.SensitiveRecordCalibrateDTO;
import com.yss.datasecurity.application.dto.SensitiveRecordVO;
import com.yss.datasecurity.application.service.SensitiveRecordAppService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SensitiveRecordController.class)
@ContextConfiguration(classes = {
    SensitiveRecordController.class,
    DataSecurityExceptionAdvice.class
})
class SensitiveRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SensitiveRecordAppService sensitiveRecordAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/sensitive-records - 分页查询识别记录列表")
    void testPageSensitiveRecords() throws Exception {
        SensitiveRecordVO vo = SensitiveRecordVO.builder()
            .id(6001L)
            .tableName("t_cust_info")
            .fieldName("mobile_phone")
            .categoryName("个人手机号码")
            .securityGradeName("L3")
            .build();

        PageResult<SensitiveRecordVO> pageResult = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(sensitiveRecordAppService.pageRecords(anyInt(), anyInt(), any(), any(), any(), any(), any()))
            .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/sensitive-records?pageIndex=1&pageSize=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(6001L))
            .andExpect(jsonPath("$.data[0].fieldName").value("mobile_phone"));
    }

    @Test
    @DisplayName("PUT /api/v1/sensitive-records - 人工校准打标返回 200")
    void testCalibrateRecord() throws Exception {
        SensitiveRecordCalibrateDTO dto = SensitiveRecordCalibrateDTO.builder()
            .recordId(6001L)
            .categoryId(2L)
            .securityGradeId(1003L)
            .lockPermanent(true)
            .build();

        doNothing().when(sensitiveRecordAppService).calibrateRecord(any(SensitiveRecordCalibrateDTO.class));

        mockMvc.perform(put("/api/v1/sensitive-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }
}
