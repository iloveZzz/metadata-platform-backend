package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datasecurity.application.dto.InstallPackageDTO;
import com.yss.datasecurity.application.dto.ProjectPackageVO;
import com.yss.datasecurity.application.dto.StaticAlgorithmVO;
import com.yss.datasecurity.application.dto.StaticMaskTestDTO;
import com.yss.datasecurity.application.dto.StaticMaskTestResultVO;
import com.yss.datasecurity.application.service.StaticMaskingAppService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaticMaskingController.class)
@ContextConfiguration(classes = {StaticMaskingController.class, DataSecurityExceptionAdvice.class})
class StaticMaskingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaticMaskingAppService staticMaskingAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/static-masking/algorithms - 查询静态算法函数列表成功")
    void testListAlgorithms() throws Exception {
        StaticAlgorithmVO vo = StaticAlgorithmVO.builder()
                .id(1L)
                .functionName("sec_mask_phone")
                .displayName("手机号码遮盖掩码")
                .algorithmType("MASK")
                .signature("sec_mask_phone(column_name)")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive"))
                .sampleOutput("138****5678")
                .build();

        when(staticMaskingAppService.listAlgorithms(any(), any())).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/v1/static-masking/algorithms?keyword=phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].functionName").value("sec_mask_phone"))
                .andExpect(jsonPath("$.data[0].algorithmType").value("MASK"));
    }

    @Test
    @DisplayName("GET /api/v1/static-masking/packages - 查询项目算法包状态成功")
    void testListProjectPackages() throws Exception {
        ProjectPackageVO vo = ProjectPackageVO.builder()
                .id(101L)
                .projectId("prj_default")
                .projectName("默认数据开发项目")
                .status("INSTALLED")
                .packageVersion("v1.5.0-standard")
                .authorizedCount(10)
                .build();

        when(staticMaskingAppService.listProjectPackages(any(), any())).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/v1/static-masking/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].projectId").value("prj_default"))
                .andExpect(jsonPath("$.data[0].status").value("INSTALLED"));
    }

    @Test
    @DisplayName("POST /api/v1/static-masking/packages/install - 安装算法包成功")
    void testInstallPackage() throws Exception {
        InstallPackageDTO dto = InstallPackageDTO.builder()
                .projectId("prj_risk")
                .packageVersion("v1.5.0-standard")
                .build();

        when(staticMaskingAppService.installPackage(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/static-masking/packages/install")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/static-masking/test-algorithm - 在线测试算法成功")
    void testTestAlgorithm() throws Exception {
        StaticMaskTestDTO dto = StaticMaskTestDTO.builder()
                .functionName("sec_mask_phone")
                .rawValue("13812345678")
                .build();

        StaticMaskTestResultVO resultVO = StaticMaskTestResultVO.builder()
                .functionName("sec_mask_phone")
                .rawValue("13812345678")
                .maskedValue("138****5678")
                .costMs(2L)
                .algorithmType("MASK")
                .sqlSnippet("SELECT sec_mask_phone(mobile) FROM target_table")
                .build();

        when(staticMaskingAppService.testAlgorithm(any())).thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/static-masking/test-algorithm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.functionName").value("sec_mask_phone"))
                .andExpect(jsonPath("$.data.maskedValue").value("138****5678"));
    }
}
