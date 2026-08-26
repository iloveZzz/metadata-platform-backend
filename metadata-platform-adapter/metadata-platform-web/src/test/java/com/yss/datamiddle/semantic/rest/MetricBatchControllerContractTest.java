package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.semantic.application.service.MetricBatchImportExportService;
import com.yss.datamiddle.semantic.client.dto.cmd.MetricBatchImportCmd;
import com.yss.datamiddle.semantic.metric.batch.MetricImportResult;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MetricBatchControllerContractTest {

    private MockMvc mockMvc;
    private MetricBatchImportExportService batchService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        batchService = Mockito.mock(MetricBatchImportExportService.class);
        MetricBatchController controller = new MetricBatchController(batchService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testImportCsvContract() throws Exception {
        MetricImportResult result = MetricImportResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .errors(Collections.emptyList())
                .build();

        Mockito.when(batchService.importFromCsv(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(result);

        MetricBatchImportCmd cmd = MetricBatchImportCmd.builder()
                .csvContent("指标名称,指标分组\n测试,财务")
                .overwriteExisting(false)
                .build();

        mockMvc.perform(post("/api/semantic/metrics/batch/import-csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.successCount").value(1));
    }

    @Test
    public void testExportCsvContract() throws Exception {
        Mockito.when(batchService.exportToCsv())
                .thenReturn("指标名称,指标分组\n净利润,财务域");

        mockMvc.perform(get("/api/semantic/metrics/batch/export-csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("净利润,财务域")));
    }
}
