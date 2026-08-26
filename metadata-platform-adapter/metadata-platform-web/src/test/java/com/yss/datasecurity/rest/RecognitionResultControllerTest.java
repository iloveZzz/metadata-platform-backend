package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RecognitionBatchLogVO;
import com.yss.datasecurity.application.dto.RecognitionResultDetailVO;
import com.yss.datasecurity.application.dto.RecognitionResultEditDTO;
import com.yss.datasecurity.application.dto.RecognitionResultImportPreviewVO;
import com.yss.datasecurity.application.dto.RecognitionResultManualAddDTO;
import com.yss.datasecurity.application.dto.RecognitionResultVO;
import com.yss.datasecurity.application.service.RecognitionResultAppService;
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
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecognitionResultController.class)
@ContextConfiguration(classes = {RecognitionResultController.class, DataSecurityExceptionAdvice.class})
class RecognitionResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecognitionResultAppService recognitionResultAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/sec/recognition-results - 分页查询识别结果列表")
    void testPageRecognitionResults() throws Exception {
        RecognitionResultVO vo = RecognitionResultVO.builder()
                .id(3001L)
                .tableName("fct_pay_order_di")
                .fieldName("pay_order_no")
                .assetSourceType("DATAPHIN")
                .assetSourceInfo("fashion_cdm_dev (服饰CDM项目)")
                .categoryId(1006L)
                .categoryName("订单信息 (/交易信息/)")
                .securityGradeId(2L)
                .securityGradeName("L2")
                .maskingStatus("ENABLED")
                .recognitionMethod("MANUAL")
                .isLocked(true)
                .hasBetterRecommendation(false)
                .build();

        PageResult<RecognitionResultVO> page = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(recognitionResultAppService.pageRecognitionResults(any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/sec/recognition-results")
                        .param("pageIndex", "1")
                        .param("pageSize", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3001))
                .andExpect(jsonPath("$.data[0].tableName").value("fct_pay_order_di"))
                .andExpect(jsonPath("$.data[0].maskingStatus").value("ENABLED"));
    }

    @Test
    @DisplayName("GET /api/v1/sec/recognition-results/{id} - 获取详情")
    void testGetDetail() throws Exception {
        RecognitionResultDetailVO detail = RecognitionResultDetailVO.builder()
                .id(3001L)
                .tableName("fct_pay_order_di")
                .fieldName("pay_order_no")
                .categoryId(1006L)
                .categoryName("订单信息")
                .securityGradeId(2L)
                .securityGradeName("L2")
                .maskingStatus("ENABLED")
                .recognitionMethod("MANUAL")
                .priority(10)
                .confidenceScore(90.0)
                .isLocked(true)
                .build();

        when(recognitionResultAppService.getRecognitionResultDetail(3001L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/sec/recognition-results/3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.tableName").value("fct_pay_order_di"));
    }

    @Test
    @DisplayName("PUT /api/v1/sec/recognition-results/{id}/masking-status - 切换状态")
    void testUpdateMaskingStatus() throws Exception {
        doNothing().when(recognitionResultAppService).updateMaskingStatus(3001L, "DISABLED");

        mockMvc.perform(put("/api/v1/sec/recognition-results/3001/masking-status")
                        .param("status", "DISABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/sec/recognition-results/{id}/lock - 锁定状态")
    void testLockResult() throws Exception {
        doNothing().when(recognitionResultAppService).lockResult(3001L, true);

        mockMvc.perform(put("/api/v1/sec/recognition-results/3001/lock")
                        .param("isLocked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-results/{id}/adopt-recommendation - 采纳推荐")
    void testAdoptRecommendation() throws Exception {
        doNothing().when(recognitionResultAppService).adoptRecommendation(3001L, null);

        mockMvc.perform(post("/api/v1/sec/recognition-results/3001/adopt-recommendation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-results/manual-add - 手动添加")
    void testManualAdd() throws Exception {
        RecognitionResultManualAddDTO dto = RecognitionResultManualAddDTO.builder()
                .dedupStrategy("OVERWRITE_ALL")
                .records(Collections.singletonList(
                        RecognitionResultManualAddDTO.ManualAddRecordItemDTO.builder()
                                .tableName("fct_trade_di")
                                .fieldName("trade_id")
                                .categoryId(1006L)
                                .maskingStatus("ENABLED")
                                .build()
                ))
                .build();

        doNothing().when(recognitionResultAppService).manualAdd(any());

        mockMvc.perform(post("/api/v1/sec/recognition-results/manual-add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/sec/recognition-results/import-preview - 导入预校验")
    void testImportPreview() throws Exception {
        RecognitionResultImportPreviewVO preview = RecognitionResultImportPreviewVO.builder()
                .totalCount(2)
                .validCount(2)
                .errorCount(0)
                .duplicateCount(0)
                .validRows(Collections.emptyList())
                .errorRows(Collections.emptyList())
                .duplicateRows(Collections.emptyList())
                .build();

        when(recognitionResultAppService.importPreview(any(), any(), any())).thenReturn(preview);

        mockMvc.perform(post("/api/v1/sec/recognition-results/import-preview")
                        .param("assetType", "DATAPHIN")
                        .param("conflictStrategy", "OVERWRITE_ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/sec/recognition-results/import-history - 查询导入历史")
    void testListImportHistory() throws Exception {
        RecognitionBatchLogVO logVO = RecognitionBatchLogVO.builder()
                .id(8001L)
                .batchType("IMPORT")
                .fileName("test.xlsx")
                .totalCount(10)
                .successCount(10)
                .status("SUCCESS")
                .build();

        when(recognitionResultAppService.listImportHistory()).thenReturn(Collections.singletonList(logVO));

        mockMvc.perform(get("/api/v1/sec/recognition-results/import-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(8001));
    }
}
