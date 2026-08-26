package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;
import com.yss.datasecurity.application.service.SecurityGradeAppService;
import com.yss.datasecurity.domain.exception.SecurityGradeReferenceConflictException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityGradeController.class)
@ContextConfiguration(classes = {SecurityGradeController.class, DataSecurityExceptionAdvice.class})
class SecurityGradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityGradeAppService securityGradeAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/security-grades - 成功返回分级列表")
    void testListSecurityGrades() throws Exception {
        SecurityGradeVO vo = SecurityGradeVO.builder()
            .id(1L)
            .gradeName("L1 对外公开")
            .gradeCode("L1")
            .sensitivityScore(20)
            .build();

        when(securityGradeAppService.listAll()).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/v1/security-grades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].gradeName").value("L1 对外公开"))
            .andExpect(jsonPath("$.data[0].sensitivityScore").value(20));
    }

    @Test
    @DisplayName("POST /api/v1/security-grades - 成功创建分级返回 201")
    void testCreateSecurityGrade() throws Exception {
        SecurityGradeCreateDTO dto = SecurityGradeCreateDTO.builder()
            .gradeName("L2_对内公开")
            .gradeCode("L2")
            .sensitivityScore(40)
            .colorTag("blue")
            .build();

        when(securityGradeAppService.create(any(SecurityGradeCreateDTO.class))).thenReturn(1002L);

        mockMvc.perform(post("/api/v1/security-grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(1002L));
    }

    @Test
    @DisplayName("PUT /api/v1/security-grades/{id} - 成功更新分级返回 200")
    void testUpdateSecurityGrade() throws Exception {
        com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO dto = com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO.builder()
            .gradeName("L2_内部数据")
            .description("更新分级描述")
            .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/security-grades/1002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/security-grades/{id} - 存在强引用时返回 409 Conflict 与 ErrorResult")
    void testDeleteSecurityGrade_Conflict() throws Exception {
        doThrow(new SecurityGradeReferenceConflictException(1004L, "L4 绝密高危", 3))
            .when(securityGradeAppService).delete(1004L);

        mockMvc.perform(delete("/api/v1/security-grades/1004"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GRADE_REFERENCE_CONFLICT"))
            .andExpect(jsonPath("$.severity").value("error"));
    }
}
