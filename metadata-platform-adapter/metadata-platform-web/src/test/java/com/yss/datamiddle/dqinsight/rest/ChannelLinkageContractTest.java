package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelFetchPort;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FetchResult;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.repository.DqAuditLogRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通道 / 关联治理契约测试（DQI-SLICE-04，冻结 OpenAPI dq-channels / dq-linkage）。
 *
 * <p>覆盖：通道 CRUD（201 / 重名 409 / 404 / 部分更新 422 / 拉取中更新 409 busy / 删除历史结果 409
 * in-use）；凭证加密密文不回传（仅 authConfigured）；重试拉取（202 + 复用切片 01 管线入库 / 失败分类 /
 * 拉取中重复 409）；待关联队列（空分页）；人工映射（422 asset.not-found / 409 already-linked +
 * confirmOverwrite / 触发健康分首次计算 + 审计 linkage-map）；通道 Token 认证端到端（真实凭证存储）。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class ChannelLinkageContractTest {

    private static final String EXECUTION_TIME = "2026-08-11T10:00:00+08:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChannelGateway channelGateway;

    @Autowired
    private DqAuditLogRepository dqAuditLogRepository;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private ChannelFetchPort channelFetchPort;

    @BeforeEach
    void setUp() {
        when(catalogAclGateway.lookupAsset("asset-ok")).thenReturn(AssetLookupResult.found("asset-ok",
                AssetSnapshot.builder().assetId("asset-ok").assetName("正常表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-resolved")).thenReturn(AssetLookupResult.found("asset-resolved",
                AssetSnapshot.builder().assetId("asset-resolved").assetName("解析后表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-missing")).thenReturn(AssetLookupResult.notFound("asset-missing"));
        when(catalogAclGateway.lookupAsset("asset-unknown")).thenReturn(AssetLookupResult.notFound("asset-unknown"));
    }

    // ---------- 通道 CRUD ----------

    @Test
    void createChannelReturnsAuthConfiguredOnlyWithoutCiphertext() throws Exception {
        mockMvc.perform(post("/api/dq/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"推送通道-1\",\"type\":\"api-push\","
                                + "\"formatType\":\"ge\",\"authToken\":\"secret-token-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("推送通道-1"))
                .andExpect(jsonPath("$.data.type").value("api-push"))
                .andExpect(jsonPath("$.data.state").value("enabled"))
                .andExpect(jsonPath("$.data.authConfigured").value(true))
                .andExpect(jsonPath("$.data.authToken").doesNotExist())
                .andExpect(jsonPath("$.data.authTokenEnc").doesNotExist()); // 密文不回传（C19）
    }

    @Test
    void createDuplicateChannelNameReturns409NameConflict() throws Exception {
        createChannel("重名通道", "api-push", "ge", null);

        mockMvc.perform(post("/api/dq/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重名通道\",\"type\":\"api-push\",\"formatType\":\"ge\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.channel.name-conflict"));
    }

    @Test
    void createScheduledPullWithoutScheduleReturns422() throws Exception {
        mockMvc.perform(post("/api/dq/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"定时通道-无周期\",\"type\":\"scheduled-pull\",\"formatType\":\"ge\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.format.invalid"));
    }

    @Test
    void updateChannelEmptyBodyReturns422() throws Exception {
        String id = createChannel("更新通道", "api-push", "ge", null);

        mockMvc.perform(put("/api/dq/channels/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateChannelPartialUpdateAndTogglePersistsState() throws Exception {
        String id = createChannel("更新通道-2", "api-push", "ge", null);

        mockMvc.perform(put("/api/dq/channels/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后的名字\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("更新后的名字"));

        mockMvc.perform(put("/api/dq/channels/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("disabled"));
    }

    @Test
    void updateChannelWhilePullingReturns409Busy() throws Exception {
        String id = createChannel("拉取中通道", "scheduled-pull", "ge", "0 * * * * *");
        IngestionChannel channel = channelGateway.findById(Long.valueOf(id)).get();
        channel.startPull();
        channelGateway.update(channel);

        mockMvc.perform(put("/api/dq/channels/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"不能改\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.channel.busy"));
    }

    @Test
    void deleteChannelWithHistoricalResultsReturns409InUse() throws Exception {
        // 带认证 Token 的通道 + 经其推送批次 → 历史结果引用
        String token = "token-delete-1";
        String id = createChannel("删除冲突通道", "api-push", "ge", token);
        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("del-batch-1", "asset-ok", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/dq/channels/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.channel.in-use"));
    }

    @Test
    void deleteChannelWithoutHistorySucceeds() throws Exception {
        String id = createChannel("可删除通道", "api-push", "ge", null);

        mockMvc.perform(delete("/api/dq/channels/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dq/channels/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ---------- 重试拉取 ----------

    @Test
    void retryPullSucceedsAndIngestsViaPipeline() throws Exception {
        when(channelFetchPort.fetch(any())).thenReturn(
                FetchResult.success(geJson("pull-batch-ok", "asset-ok", EXECUTION_TIME, allPassedRules()),
                        "application/json"));
        String id = createChannel("拉取成功通道", "scheduled-pull", "ge", "0 * * * * *");

        mockMvc.perform(post("/api/dq/channels/{id}/retry", id))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("enabled"))
                .andExpect(jsonPath("$.data.lastPullAt").isNotEmpty())
                .andExpect(jsonPath("$.data.lastError").doesNotExist());

        // 复用切片 01 管线入库可验证
        mockMvc.perform(get("/api/dq/results").param("channelId", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].batchNo").value("pull-batch-ok"));
    }

    @Test
    void retryPullFailureClassifiesAndRecordsError() throws Exception {
        when(channelFetchPort.fetch(any())).thenReturn(
                FetchResult.failure(ErrorCategory.NETWORK, "拉取网络失败（连接 / 超时）"));
        String id = createChannel("拉取失败通道", "scheduled-pull", "ge", "0 * * * * *");

        mockMvc.perform(post("/api/dq/channels/{id}/retry", id))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("pull-failed"))
                .andExpect(jsonPath("$.data.errorCategory").value("network"))
                .andExpect(jsonPath("$.data.lastError").isNotEmpty());
    }

    @Test
    void retryPullWhilePullingReturns409Busy() throws Exception {
        String id = createChannel("重试冲突通道", "scheduled-pull", "ge", "0 * * * * *");
        IngestionChannel channel = channelGateway.findById(Long.valueOf(id)).get();
        channel.startPull();
        channelGateway.update(channel);

        mockMvc.perform(post("/api/dq/channels/{id}/retry", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.channel.busy"));
    }

    // ---------- 待关联队列 + 人工映射 ----------

    @Test
    void pendingQueueEmptyReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/dq/asset-linkage/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void mapLinkageAssetNotFoundReturns422() throws Exception {
        String token = "token-map-1";
        createChannel("映射通道", "api-push", "ge", token);
        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("map-batch-1", "asset-missing", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());
        String linkageId = firstPendingLinkageId();

        mockMvc.perform(post("/api/dq/asset-linkage/{id}/map", linkageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-unknown\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.asset.not-found"));
    }

    @Test
    void mapLinkageSucceedsTriggersFirstCalcAndAudits() throws Exception {
        String token = "token-map-2";
        createChannel("映射通道-2", "api-push", "ge", token);
        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("map-batch-2", "asset-missing", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());
        String linkageId = firstPendingLinkageId();

        MvcResult result = mockMvc.perform(post("/api/dq/asset-linkage/{id}/map", linkageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-resolved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("asset-resolved"))
                .andExpect(jsonPath("$.data.assetName").value("解析后表"))
                .andExpect(jsonPath("$.data.domain").value("交易域"))
                .andExpect(jsonPath("$.data.state").value("linked"))
                .andExpect(jsonPath("$.data.matchMode").value("manual"))
                .andReturn();

        // 触发健康分首次计算（复用切片 02 计算入口）
        mockMvc.perform(get("/api/dq/health").param("assetId", "asset-resolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].band").value("优"))
                .andExpect(jsonPath("$.data[0].score").value(100));

        // 审计 linkage-map 留痕（只读不可变；查询端点属切片 05，此处仓储直查）
        List<DqAuditLogPO> audits = dqAuditLogRepository.selectList(null);
        assertThat(audits).anySatisfy(e -> {
            assertThat(e.getAction()).isEqualTo("linkage-map");
            assertThat(e.getObject()).isEqualTo("map-batch-2");
        });
    }

    @Test
    void mapAlreadyLinkedRequiresConfirmOverwrite() throws Exception {
        String token = "token-map-3";
        createChannel("映射通道-3", "api-push", "ge", token);
        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("map-batch-3", "asset-missing", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());
        String linkageId = firstPendingLinkageId();

        // 首次映射成功
        mockMvc.perform(post("/api/dq/asset-linkage/{id}/map", linkageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-resolved\"}"))
                .andExpect(status().isOk());

        // 未确认覆盖 → 409
        mockMvc.perform(post("/api/dq/asset-linkage/{id}/map", linkageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-ok\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.linkage.already-linked"));

        // confirmOverwrite=true 二次确认 → 200 覆盖
        mockMvc.perform(post("/api/dq/asset-linkage/{id}/map", linkageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"asset-ok\",\"confirmOverwrite\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("asset-ok"));
    }

    // ---------- 通道 Token 认证端到端（真实凭证存储闭环切片 01 seam） ----------

    @Test
    void channelTokenAuthEndToEndViaRealCredentialStore() throws Exception {
        String token = "token-e2e-auth";
        createChannel("认证通道", "api-push", "ge", token);

        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("auth-batch-1", "asset-ok", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        // 错误 Token → 422 err.dq.auth.invalid（脱敏）
        mockMvc.perform(post("/api/dq/results").header("Authorization", "Bearer wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("auth-batch-2", "asset-ok", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.auth.invalid"));
    }

    // ---------- helpers ----------

    private String createChannel(String name, String type, String formatType, String token) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("{\"name\":\"").append(name).append("\",\"type\":\"").append(type)
                .append("\",\"formatType\":\"").append(formatType).append("\"");
        if (token != null) {
            body.append(",\"authToken\":\"").append(token).append("\"");
        }
        if ("scheduled-pull".equals(type)) {
            body.append(",\"schedule\":\"0 * * * * *\"");
        }
        body.append("}");
        MvcResult result = mockMvc.perform(post("/api/dq/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String firstPendingLinkageId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dq/asset-linkage/pending"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.get(0).path("id").asText();
    }

    private static String allPassedRules() {
        return passedRule("", "非空率", "non-null-rate") + "," + passedRule("", "格式", "format")
                + "," + passedRule("", "唯一性", "uniqueness") + "," + passedRule("", "值域", "value-range")
                + "," + passedRule("", "新鲜度", "freshness");
    }

    private static String passedRule(String fieldName, String ruleName, String ruleType) {
        return "{\"ruleName\":\"" + ruleName + "\",\"ruleType\":\"" + ruleType
                + "\",\"status\":\"passed\"" + fieldJson(fieldName) + "}";
    }

    private static String fieldJson(String fieldName) {
        return (fieldName == null || fieldName.isEmpty()) ? "" : ",\"fieldName\":\"" + fieldName + "\"";
    }

    private static String geJson(String batchNo, String assetId, String executionTime, String rules) {
        return "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"" + batchNo + "\","
                + "\"executionTime\":\"" + executionTime + "\","
                + "\"assetId\":\"" + assetId + "\",\"results\":[" + rules + "]}";
    }
}
