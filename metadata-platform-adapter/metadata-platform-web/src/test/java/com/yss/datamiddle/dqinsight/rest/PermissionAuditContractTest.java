package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.repository.DqChannelRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqChannelPO;
import com.yss.datamiddle.dqinsight.repository.gateway.impl.DqRbacProperties;
import com.yss.datamiddle.dqinsight.rest.filter.TokenFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全端点权限契约测试（DQI-SLICE-05-WU1，C24 安全红线 / DQI-007）。
 *
 * <p>当前用户可见域 = 交易域（dq.rbac.visible-domains=交易域）：仪表盘 / 健康分列表 /
 * 详情 / 钻取 / 待关联队列域外不展示（浏览隐藏）；直连域外详情 403 err.dq.forbidden 且
 * 错误信息不含资源标识（不泄露存在性）；操作类端点（通道新建 / 配置 / 启停 / 重试、
 * 人工映射、审计查询）无权限越权调用 403（dq.rbac.deny-capabilities 全拒绝）。
 * 用例级事务回滚保证数据隔离。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "dq.rbac.visible-domains=交易域",
        "dq.rbac.deny-capabilities=channel:create,channel:update,channel:delete,channel:retry,linkage:map,audit:query"
})
class PermissionAuditContractTest {

    private static final String TOKEN_TRADE = "channel-token-perm-trade";
    private static final String TOKEN_RISK = "channel-token-perm-risk";
    private static final String BEARER_TRADE = "Bearer " + TOKEN_TRADE;
    private static final String BEARER_RISK = "Bearer " + TOKEN_RISK;
    private static final String EXECUTION_TIME = "2026-08-11T10:00:00+08:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DqChannelRepository dqChannelRepository;

    @Autowired
    private DqRbacProperties rbacProperties;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private ChannelCredentialStore channelCredentialStore;

    @BeforeEach
    void setUp() {
        // 通道凭证：交易域通道 1001 / 风控域通道 1002（pending 域过滤 = 来源通道域）
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(TOKEN_TRADE)))
                .thenReturn(Optional.of(new ChannelCredential("1001", "ak-1", true)));
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(TOKEN_RISK)))
                .thenReturn(Optional.of(new ChannelCredential("1002", "ak-2", true)));
        // 资产防腐层：交易域 / 风控域资产命中，其余（asset-miss-*）未命中挂 pending。
        // 注意：不使用 anyString 默认 stub（Mockito 最后注册的匹配 stub 生效，会覆盖精确 stub）
        when(catalogAclGateway.lookupAsset("asset-trade")).thenReturn(AssetLookupResult.found("asset-trade",
                AssetSnapshot.builder().assetId("asset-trade").assetName("用户表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-risk")).thenReturn(AssetLookupResult.found("asset-risk",
                AssetSnapshot.builder().assetId("asset-risk").assetName("风控名单表").domain("风控域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-miss-trade"))
                .thenReturn(AssetLookupResult.notFound("asset-miss-trade"));
        when(catalogAclGateway.lookupAsset("asset-miss-risk"))
                .thenReturn(AssetLookupResult.notFound("asset-miss-risk"));
        when(catalogAclGateway.countVisibleTargetAssets(any())).thenReturn(10);
        // 通道（pending 域过滤来源）
        dqChannelRepository.insert(channel(1001L, "交易通道", "交易域"));
        dqChannelRepository.insert(channel(1002L, "风控通道", "风控域"));
        // 可见域配置绑定断言（中文域经 @TestPropertySource 绑定，编码校验）
        org.assertj.core.api.Assertions.assertThat(rbacProperties.getVisibleDomains())
                .containsExactly("交易域");
    }

    // ---------- 数据域可见性：浏览隐藏 ----------

    @Test
    void healthListHidesOutOfDomainAssets() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-1", "asset-trade");
        ingest(BEARER_RISK, "perm-risk-1", "asset-risk");

        MvcResult result = mockMvc.perform(get("/api/dq/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andReturn();
        JsonNode rows = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        // 仅交易域资产可见，风控域资产不展示（域外隐藏）
        org.assertj.core.api.Assertions.assertThat(rows).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(rows.get(0).path("assetId").asText()).isEqualTo("asset-trade");
        org.assertj.core.api.Assertions.assertThat(rows.get(0).path("domain").asText()).isEqualTo("交易域");
    }

    @Test
    void dashboardAggregatesVisibleDomainOnly() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-2", "asset-trade");
        ingest(BEARER_RISK, "perm-risk-2", "asset-risk");

        mockMvc.perform(get("/api/dq/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets.totalCount").value(1))
                .andExpect(jsonPath("$.data.assets.data.length()").value(1))
                .andExpect(jsonPath("$.data.assets.data[0].assetId").value("asset-trade"))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(1));
    }

    @Test
    void dashboardDomainFilterIntersectsWithVisibleDomains() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-3", "asset-trade");
        ingest(BEARER_RISK, "perm-risk-3", "asset-risk");

        // 域筛选参数为域外域 → 与可见域交集为空 → 空资产（不泄露域外）
        mockMvc.perform(get("/api/dq/dashboard").param("domain", "风控域"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets.totalCount").value(0))
                .andExpect(jsonPath("$.data.assets.data.length()").value(0));
    }

    @Test
    void directOutOfDomainDetailReturns403WithoutLeakingResource() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-4", "asset-trade");
        ingest(BEARER_RISK, "perm-risk-4", "asset-risk");

        MvcResult result = mockMvc.perform(get("/api/dq/health/asset-risk"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // 不泄露域外资源存在性：错误信息不含资源标识
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("asset-risk");
    }

    @Test
    void inDomainDetailReturns200() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-5", "asset-trade");

        MvcResult result = mockMvc.perform(get("/api/dq/health/asset-trade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("asset-trade"))
                .andReturn();
        JsonNode detail = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        org.assertj.core.api.Assertions.assertThat(detail.path("domain").asText()).isEqualTo("交易域");
    }

    @Test
    void outOfDomainRuleDetailReturns403() throws Exception {
        ingest(BEARER_TRADE, "perm-trade-6", "asset-trade");
        ingest(BEARER_RISK, "perm-risk-6", "asset-risk");

        mockMvc.perform(get("/api/dq/health/asset-risk/details"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void pendingQueueShowsOnlyVisibleDomainChannelSources() throws Exception {
        // 交易域通道批次 → pending（交易域可见）；风控域通道批次 → pending（域外隐藏）
        ingest(BEARER_TRADE, "perm-pending-trade", "asset-miss-trade");
        ingest(BEARER_RISK, "perm-pending-risk", "asset-miss-risk");

        MvcResult result = mockMvc.perform(get("/api/dq/asset-linkage/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andReturn();
        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        org.assertj.core.api.Assertions.assertThat(rows).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(rows.get(0).path("assetId").asText()).isEqualTo("asset-miss-trade");
    }

    // ---------- 操作权限：无权限越权调用 403 ----------

    @Test
    void channelCreateDenied403() throws Exception {
        mockMvc.perform(post("/api/dq/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"通道X\",\"type\":\"scheduled-pull\",\"schedule\":\"0 * * * * *\","
                                + "\"formatType\":\"ge\",\"domain\":\"交易域\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void channelUpdateDenied403() throws Exception {
        mockMvc.perform(put("/api/dq/channels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"改名\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void channelDeleteDenied403() throws Exception {
        mockMvc.perform(delete("/api/dq/channels/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void channelRetryDenied403() throws Exception {
        mockMvc.perform(post("/api/dq/channels/1/retry"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void linkageMapDenied403() throws Exception {
        mockMvc.perform(post("/api/dq/asset-linkage/1/map")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-trade\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    @Test
    void auditLogsDenied403() throws Exception {
        mockMvc.perform(get("/api/dq/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("err.dq.forbidden"));
    }

    // ---------- helpers ----------

    private void ingest(String bearer, String batchNo, String assetId) throws Exception {
        mockMvc.perform(post("/api/dq/results").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson(batchNo, assetId)))
                .andExpect(status().isCreated());
    }

    private static DqChannelPO channel(Long id, String name, String domain) {
        DqChannelPO po = new DqChannelPO();
        po.setId(id);
        po.setName(name);
        po.setType("scheduled-pull");
        po.setFormatType("ge");
        po.setDomain(domain);
        po.setState("enabled");
        po.setCreatedAt(java.time.LocalDateTime.now());
        po.setUpdatedAt(java.time.LocalDateTime.now());
        return po;
    }

    private static String geJson(String batchNo, String assetId) {
        return "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"" + batchNo + "\","
                + "\"executionTime\":\"" + EXECUTION_TIME + "\",\"channelId\":\"ch-1\","
                + "\"assetId\":\"" + assetId + "\",\"results\":["
                + "{\"ruleName\":\"非空率\",\"ruleType\":\"non-null-rate\",\"status\":\"passed\"},"
                + "{\"ruleName\":\"格式\",\"ruleType\":\"format\",\"status\":\"passed\"},"
                + "{\"ruleName\":\"唯一性\",\"ruleType\":\"uniqueness\",\"status\":\"passed\"},"
                + "{\"ruleName\":\"值域\",\"ruleType\":\"value-range\",\"status\":\"passed\"},"
                + "{\"ruleName\":\"新鲜度\",\"ruleType\":\"freshness\",\"status\":\"passed\"}]}";
    }
}
