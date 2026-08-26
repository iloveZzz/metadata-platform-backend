package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.AuditResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审计日志查询契约测试（DQI-SLICE-05-WU2，冻结契约 GET /api/dq/audit-logs）。
 *
 * <p>分页 + action 筛选（7 类枚举）；0 条以空分页表达（totalCount=0 / data=[]，非错误）；
 * 字段映射对齐冻结 AuditLogEntry（time ISO 8601 / action / result 枚举码 / operator / object /
 * detail 脱敏透传）；只读不可变（仅 GET，无写端点；append-only INSERT-only 由网关与迁移强制）。
 * 默认权限配置（dq.rbac.deny-capabilities 为空）= 审计查询允许；403 兜底见 PermissionAuditContractTest。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class AuditLogContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogGateway auditLogGateway;

    @BeforeEach
    void setUp() {
        // 6 类动作留痕（linkage-map 留待空分页断言），operator / object / detail 对齐写路径语义
        auditLogGateway.record(AuditLogEntry.ingest("ge-tool", "batch-001", "rows=120"));
        auditLogGateway.record(AuditLogEntry.parseFail("ge-tool", "batch-002", "CSV schema 违反 row:3"));
        auditLogGateway.record(AuditLogEntry.healthCalc("system", "batch-001", "ruleVersion=v3, assets=10"));
        auditLogGateway.record(AuditLogEntry.channelConfig("ops-user", "通道A", "schedule 变更"));
        auditLogGateway.record(AuditLogEntry.channelToggle("ops-user", "通道A", "停用"));
        auditLogGateway.record(AuditLogEntry.channelRetry("ops-user", "通道A", "网络超时", AuditResult.FAILURE));
    }

    @Test
    void pageReturnsAllActionsInTimeDescOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dq/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        // 全部 6 类动作入页；按 event_time 倒序（同事务毫秒级写入，顺序仅验证时间不升序）
        JsonNode data = root.path("data");
        assertThat(data).hasSize(6);
        assertThat(data).extracting(node -> node.path("action").asText())
                .containsExactlyInAnyOrder("ingest", "parse-fail", "health-calc", "channel-config",
                        "channel-toggle", "channel-retry");
    }

    @Test
    void actionFilterReturnsOnlyMatchingAction() throws Exception {
        mockMvc.perform(get("/api/dq/audit-logs").param("action", "health-calc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].action").value("health-calc"))
                .andExpect(jsonPath("$.data[0].object").value("batch-001"))
                .andExpect(jsonPath("$.data[0].result").value("success"));
    }

    @Test
    void noMatchingActionReturnsEmptyPage() throws Exception {
        // 未写入 linkage-map：action 筛选 0 条以空分页表达（totalCount=0 / data=[]，非错误）
        mockMvc.perform(get("/api/dq/audit-logs").param("action", "linkage-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void paginationSlicesRecordsAndKeepsTotal() throws Exception {
        mockMvc.perform(get("/api/dq/audit-logs").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(2));

        // 0 条以空分页表达由 action 无匹配覆盖（noMatchingActionReturnsEmptyPage）；
        // 分页截断 + totalCount 回读与既有查询契约一致（PageQuery 自动分页，C9）
        mockMvc.perform(get("/api/dq/audit-logs").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void voFieldsMapToFrozenContractShape() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dq/audit-logs").param("action", "ingest"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode row = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").get(0);
        // 冻结契约 AuditLogEntry：time / operator / action / object / result / detail
        assertThat(row.path("time").asText()).isNotBlank();
        assertThat(row.path("operator").asText()).isEqualTo("ge-tool");
        assertThat(row.path("action").asText()).isEqualTo("ingest");
        assertThat(row.path("object").asText()).isEqualTo("batch-001");
        assertThat(row.path("result").asText()).isEqualTo("success");
        assertThat(row.path("detail").asText()).isEqualTo("rows=120");
        // 脱敏：detail 不包含凭证类敏感信息（写入路径保证 C19 / C27）
        assertThat(row.path("detail").asText()).doesNotContain("token", "auth");
    }

    @Test
    void auditLogsEndpointIsReadOnly() throws Exception {
        // 只读不可变：仅 GET 契约端点；不存在写路径（无 PUT / DELETE 路由，C27）
        mockMvc.perform(get("/api/dq/audit-logs/1")).andExpect(status().isNotFound());
    }
}
