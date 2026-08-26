package com.yss.metadata.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.metadata.application.collector.service.CollectorInstanceAppService;
import com.yss.metadata.client.dto.cmd.BatchInstanceCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceRerunCmd;
import com.yss.metadata.client.dto.cmd.CollectorInstanceTerminateCmd;
import com.yss.metadata.client.dto.query.CollectorInstanceQuery;
import com.yss.metadata.client.vo.CollectorInstanceVO;
import com.yss.metadata.client.vo.MetadataDiffSummaryVO;
import com.yss.metadata.client.vo.WorkflowNodeVO;
import com.yss.metadata.domain.collector.exception.CollectorInstanceStateConflictException;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 采集实例 REST 契约与业务行为测试。
 */
class CollectorInstanceControllerTest {

    private static final String INSTANCES_PATH = "/api/collector-instances";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;
    private CollectorInstanceAppService appService;

    @BeforeEach
    void setUp() {
        appService = Mockito.mock(CollectorInstanceAppService.class);
        CollectorInstanceController controller = new CollectorInstanceController(appService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/collector-instances 查询列表成功返回 YSS MultiResult")
    void shouldListAllInstances() throws Exception {
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-1")
                .name("MySQL采集demo")
                .status("success")
                .build();
        when(appService.list(any())).thenReturn(Arrays.asList(vo, vo, vo, vo, vo));

        mockMvc.perform(get(INSTANCES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("GET /api/collector-instances?onlyFailed=true 仅过滤失败实例")
    void shouldFilterFailedInstances() throws Exception {
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-failed")
                .status("failed")
                .build();
        when(appService.list(any())).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get(INSTANCES_PATH).param("onlyFailed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data[0].status", is("failed")));
    }

    @Test
    @DisplayName("GET /api/collector-instances?owner=1397905662202719 过滤我负责的任务实例")
    void shouldFilterByOwner() throws Exception {
        when(appService.list(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get(INSTANCES_PATH).param("owner", "1397905662202719"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")));
    }

    @Test
    @DisplayName("GET /api/collector-instances?keyword=MySQL 关键字模糊匹配")
    void shouldFilterByKeyword() throws Exception {
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-mysql")
                .datasourceType("MySQL")
                .build();
        when(appService.list(any())).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get(INSTANCES_PATH).param("keyword", "MySQL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data[0].datasourceType", is("MySQL")));
    }

    @Test
    @DisplayName("GET /api/collector-instances/{id} 获取详情")
    void shouldGetInstanceDetail() throws Exception {
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-qbi-report")
                .status("success")
                .build();
        when(appService.getById("inst-qbi-report")).thenReturn(vo);

        mockMvc.perform(get(INSTANCES_PATH + "/inst-qbi-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data.id", is("inst-qbi-report")))
                .andExpect(jsonPath("$.data.status", is("success")));
    }

    @Test
    @DisplayName("GET /api/collector-instances/{id}/diff-summary 获取变更概览")
    void shouldGetDiffSummary() throws Exception {
        MetadataDiffSummaryVO.TableDiffVO item = MetadataDiffSummaryVO.TableDiffVO.builder()
                .tableName("crm_customer_profile_v2")
                .build();
        MetadataDiffSummaryVO diff = MetadataDiffSummaryVO.builder()
                .instanceId("inst-qbi-report")
                .totalObjects(128)
                .addedObjects(15)
                .tableDetails(Collections.singletonList(item))
                .build();
        when(appService.getDiffSummary("inst-qbi-report")).thenReturn(diff);

        mockMvc.perform(get(INSTANCES_PATH + "/inst-qbi-report/diff-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data.totalObjects", is(128)))
                .andExpect(jsonPath("$.data.addedObjects", is(15)))
                .andExpect(jsonPath("$.data.tableDetails[0].tableName", is("crm_customer_profile_v2")));
    }

    @Test
    @DisplayName("POST /api/collector-instances/{id}/rerun 失败实例重跑成功")
    void shouldRerunFailedInstance() throws Exception {
        CollectorInstanceRerunCmd cmd = CollectorInstanceRerunCmd.builder().operator("1397905662202719").build();
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-bird-20250113")
                .status("running")
                .build();
        when(appService.rerun(eq("inst-bird-20250113"), any())).thenReturn(vo);

        mockMvc.perform(post(INSTANCES_PATH + "/inst-bird-20250113/rerun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data.status", is("running")));
    }

    @Test
    @DisplayName("POST /api/collector-instances/{id}/rerun 非失败实例重跑抛状态冲突 409")
    void shouldRejectRerunNonFailedInstance() throws Exception {
        when(appService.rerun(eq("inst-qbi-report"), any()))
                .thenThrow(new CollectorInstanceStateConflictException("仅失败状态的实例支持重跑"));

        mockMvc.perform(post(INSTANCES_PATH + "/inst-qbi-report/rerun")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/collector-instances/batch-rerun 批量重跑")
    void shouldBatchRerunFailedInstances() throws Exception {
        BatchInstanceCmd cmd = BatchInstanceCmd.builder()
                .instanceIds(Arrays.asList("inst-qbi-report", "inst-bird-20250113"))
                .operator("1397905662202719")
                .build();
        when(appService.batchRerun(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(post(INSTANCES_PATH + "/batch-rerun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")));
    }

    @Test
    @DisplayName("POST /api/collector-instances/{id}/terminate 运行中实例终止成功并置为失败")
    void shouldTerminateRunningInstance() throws Exception {
        CollectorInstanceTerminateCmd cmd = CollectorInstanceTerminateCmd.builder()
                .operator("admin")
                .reason("数据源配置错误需提前中断")
                .build();
        CollectorInstanceVO vo = CollectorInstanceVO.builder()
                .id("inst-mysql-demo")
                .status("failed")
                .build();
        when(appService.terminate(eq("inst-mysql-demo"), any())).thenReturn(vo);

        mockMvc.perform(post(INSTANCES_PATH + "/inst-mysql-demo/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data.status", is("failed")));
    }

    @Test
    @DisplayName("POST /api/collector-instances/{id}/terminate 成功实例不可终止，抛 409")
    void shouldRejectTerminateSuccessInstance() throws Exception {
        when(appService.terminate(eq("inst-qbi-report"), any()))
                .thenThrow(new CollectorInstanceStateConflictException("仅运行中或等待中的实例支持终止"));

        mockMvc.perform(post(INSTANCES_PATH + "/inst-qbi-report/terminate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/collector-instances/{id}/nodes 获取工作流节点及 Dlink 诊断信息")
    void shouldGetWorkflowNodesAndDiagnostics() throws Exception {
        Map<String, Object> perf = new HashMap<>();
        perf.put("throughput", "12,450 records/sec");

        WorkflowNodeVO node1 = WorkflowNodeVO.builder()
                .id("node-1")
                .type("jdbc_probe")
                .status("success")
                .build();
        WorkflowNodeVO node2 = WorkflowNodeVO.builder()
                .id("node-2")
                .type("dlink")
                .status("success")
                .performanceMetrics(perf)
                .build();
        when(appService.getWorkflowNodes("inst-qbi-report")).thenReturn(Arrays.asList(node1, node2));

        mockMvc.perform(get(INSTANCES_PATH + "/inst-qbi-report/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data[1].type", is("dlink")))
                .andExpect(jsonPath("$.data[1].performanceMetrics.throughput", is("12,450 records/sec")));
    }

    @Test
    @DisplayName("POST /api/collector-instances/{id}/nodes/{nodeId}/rerun 单节点重跑")
    void shouldRerunWorkflowNode() throws Exception {
        WorkflowNodeVO node = WorkflowNodeVO.builder()
                .id("node-2")
                .status("running")
                .build();
        when(appService.rerunWorkflowNode("inst-bird-20250113", "inst-bird-20250113-node-2", null))
                .thenReturn(node);

        mockMvc.perform(post(INSTANCES_PATH + "/inst-bird-20250113/nodes/inst-bird-20250113-node-2/rerun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("DM-A0001")))
                .andExpect(jsonPath("$.data.status", is("running")));
    }
}
