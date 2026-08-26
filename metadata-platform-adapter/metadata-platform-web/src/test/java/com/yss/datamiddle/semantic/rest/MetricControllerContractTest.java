package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.datamiddle.semantic.application.model.MetricCreateInput;
import com.yss.datamiddle.semantic.application.model.MetricVersionInput;
import com.yss.datamiddle.semantic.application.service.MetricService;
import com.yss.datamiddle.semantic.metric.exception.MetricNameDuplicateException;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import com.yss.datamiddle.semantic.rest.exception.SemanticExceptionAdvice;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MetricControllerContractTest {

    @Mock
    private MetricService metricService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MetricController controller = new MetricController(metricService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SemanticExceptionAdvice(new com.yss.datamiddle.semantic.rest.convertor.TermWebConvertorImpl()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("CT-01: 创建指标口径成功")
    void createMetricSuccess() throws Exception {
        MetricDefinition m = MetricDefinition.create("GMV", "gmv_group", "成交总额", "finance", "u1");
        m.setId(1L);
        when(metricService.create(any(MetricCreateInput.class))).thenReturn(m);

        String json = "{\"name\":\"GMV\",\"metricGroup\":\"gmv_group\",\"description\":\"成交总额\",\"owner\":\"finance\"}";

        mockMvc.perform(post("/api/semantic/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.name").value("GMV"));
    }

    @Test
    @DisplayName("CT-03: 同名创建返回 422 METRIC_NAME_DUPLICATE")
    void createMetricDuplicateThrows422() throws Exception {
        when(metricService.create(any(MetricCreateInput.class)))
                .thenThrow(new MetricNameDuplicateException("GMV"));

        String json = "{\"name\":\"GMV\",\"owner\":\"finance\"}";

        mockMvc.perform(post("/api/semantic/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("METRIC_NAME_DUPLICATE"));
    }

    @Test
    @DisplayName("CT-05: 新增版本成为当前版本")
    void addVersionSuccess() throws Exception {
        MetricVersion v = MetricVersion.builder()
                .versionNo(1)
                .expression("sum(amt)")
                .logicDescription("汇总")
                .build();
        when(metricService.addVersion(anyLong(), any(MetricVersionInput.class))).thenReturn(v);

        String json = "{\"expression\":\"sum(amt)\",\"logicDescription\":\"汇总\"}";

        mockMvc.perform(post("/api/semantic/metric-definitions/1/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.expression").value("sum(amt)"));
    }

    @Test
    @DisplayName("CT-06: 版本回滚生成新版本")
    void rollbackVersionSuccess() throws Exception {
        MetricVersion v = MetricVersion.builder()
                .versionNo(3)
                .expression("sum(amt)")
                .rollbackFromNo(1)
                .build();
        when(metricService.rollback(anyLong(), anyInt())).thenReturn(v);

        mockMvc.perform(post("/api/semantic/metric-definitions/1/versions/1/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.versionNo").value(3))
                .andExpect(jsonPath("$.data.rollbackFromNo").value(1));
    }

    @Test
    @DisplayName("CT-08: 认证冲突 force=false 返回 409 AUTH_CONFLICT")
    void certifyConflictReturns409() throws Exception {
        when(metricService.certify(anyLong(), anyBoolean()))
                .thenThrow(new StateConflictException("AUTH_CONFLICT: 该指标组已存在认证口径"));

        String json = "{\"force\":false}";

        mockMvc.perform(post("/api/semantic/metric-definitions/1/certify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }
}
