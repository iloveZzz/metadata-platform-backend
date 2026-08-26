package com.yss.metadata.rest;

import com.yss.metadata.application.integration.service.impl.OpenLineageIngestionServiceImpl;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.InMemoryAssetGateway;
import com.yss.metadata.rest.support.InMemoryLineageGraphRepository;
import com.yss.metadata.rest.support.InMemoryOpenLineageEventGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenLineage 事件接收 REST 契约测试（WU-05-05，冻结 OpenAPI /api/v1/lineage 段）。
 *
 * <p>覆盖：合法事件 202（事件已接收，无响应体）、校验失败 422（缺 eventType /
 * run.runId / job）、未知枚举 422、解析成功写入事件记录与血缘。</p>
 */
class OpenLineageControllerTest {

    private InMemoryOpenLineageEventGateway eventGateway;
    private InMemoryAssetGateway assetGateway;
    private InMemoryLineageGraphRepository graphRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventGateway = new InMemoryOpenLineageEventGateway();
        assetGateway = new InMemoryAssetGateway();
        graphRepository = new InMemoryLineageGraphRepository();
        OpenLineageIngestionServiceImpl service =
                new OpenLineageIngestionServiceImpl(eventGateway, assetGateway, graphRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new OpenLineageController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/lineage 合法 COMPLETE 事件：202 事件已接收（无响应体）")
    void validEventReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"COMPLETE\","
                                + "\"run\":{\"runId\":\"run-1\"},"
                                + "\"job\":{\"namespace\":\"ns1\",\"name\":\"job1\"},"
                                + "\"inputs\":[{\"namespace\":\"ns1\",\"name\":\"ods_order\"}],"
                                + "\"outputs\":[{\"namespace\":\"ns2\",\"name\":\"dwd_order_di\"}]}"))
                .andExpect(status().isAccepted());

        // 事件已记录且解析成功；血缘边已写入
        assertThat(eventGateway.all()).hasSize(1);
        assertThat(eventGateway.all().get(0).getParseStatus()).isEqualTo(OpenLineageParseStatus.PARSED);
        assertThat(graphRepository.allEdges()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/lineage 缺 eventType：422 param.invalid")
    void missingEventTypeReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"run\":{\"runId\":\"run-1\"},"
                                + "\"job\":{\"namespace\":\"ns1\",\"name\":\"job1\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("eventType")));
        assertThat(eventGateway.all()).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/lineage 缺 run.runId：422")
    void missingRunIdReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"COMPLETE\","
                                + "\"job\":{\"namespace\":\"ns1\",\"name\":\"job1\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("run.runId")));
    }

    @Test
    @DisplayName("POST /api/v1/lineage 缺 job.namespace：422")
    void missingJobReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"COMPLETE\","
                                + "\"run\":{\"runId\":\"run-1\"},"
                                + "\"job\":{\"name\":\"job1\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("job")));
    }

    @Test
    @DisplayName("POST /api/v1/lineage 未知事件类型：422（枚举解析失败，请求体不可读）")
    void unknownEventTypeReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/lineage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"UNKNOWN\","
                                + "\"run\":{\"runId\":\"run-1\"},"
                                + "\"job\":{\"namespace\":\"ns1\",\"name\":\"job1\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"));
        assertThat(eventGateway.all()).isEmpty();
    }
}
