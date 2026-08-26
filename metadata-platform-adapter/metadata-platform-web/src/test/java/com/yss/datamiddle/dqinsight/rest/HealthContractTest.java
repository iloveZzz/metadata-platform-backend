package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.repository.DqAuditLogRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;
import com.yss.datamiddle.dqinsight.rest.filter.TokenFingerprint;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 健康分契约测试（DQI-SLICE-02-WU3 + WU1/WU2 端到端）。
 *
 * <p>覆盖：接入 → 计算触发 seam 闭环（真实触发实现）→ 档位 / 规则版本 / 加权计算 / 过期态 /
 * 无结果独立展示态 / 404 语义 / 分页筛选 / 规则明细钻取（分数来源区公式与权重）/ 计算审计 health-calc。
 * 用例级事务回滚保证数据隔离。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class HealthContractTest {

    private static final String VALID_TOKEN = "channel-token-valid-002";
    private static final String BEARER_VALID = "Bearer " + VALID_TOKEN;
    private static final String EXECUTION_TIME = "2026-08-11T10:00:00+08:00";
    private static final String EXPIRED_EXECUTION_TIME = "2026-06-01T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DqAuditLogRepository dqAuditLogRepository;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private ChannelCredentialStore channelCredentialStore;

    @BeforeEach
    void setUp() {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(VALID_TOKEN)))
                .thenReturn(Optional.of(new ChannelCredential("ch-1", "ak-1", true)));
        when(catalogAclGateway.lookupAsset("asset-1")).thenReturn(AssetLookupResult.found("asset-1",
                AssetSnapshot.builder().assetId("asset-1").assetName("用户表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-2")).thenReturn(AssetLookupResult.found("asset-2",
                AssetSnapshot.builder().assetId("asset-2").assetName("订单表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-3")).thenReturn(AssetLookupResult.found("asset-3",
                AssetSnapshot.builder().assetId("asset-3").assetName("风控名单表").domain("风控域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-expired")).thenReturn(AssetLookupResult.found("asset-expired",
                AssetSnapshot.builder().assetId("asset-expired").assetName("历史表").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-fields")).thenReturn(AssetLookupResult.found("asset-fields",
                AssetSnapshot.builder().assetId("asset-fields").assetName("字段表").domain("交易域")
                        .assetType("table").build()));
    }

    @Test
    void linkedIngestTriggersHealthScoreCalculationAndListShowsBand() throws Exception {
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-1", "asset-1", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dq/health").param("assetId", "asset-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].assetId").value("asset-1"))
                .andExpect(jsonPath("$.data[0].state").value("ok"))
                .andExpect(jsonPath("$.data[0].score").value(100))
                .andExpect(jsonPath("$.data[0].band").value("优"))
                .andExpect(jsonPath("$.data[0].expired").value(false))
                .andExpect(jsonPath("$.data[0].hasResult").value(true))
                .andExpect(jsonPath("$.data[0].passRate").value("100%"))
                .andExpect(jsonPath("$.data[0].validUntil").isNotEmpty())
                .andExpect(jsonPath("$.totalCount").value(1));

        // 计算审计 health-calc 留痕（记录规则版本与结果）
        List<DqAuditLogPO> auditEntries = dqAuditLogRepository.selectList(null);
        assertThat(auditEntries)
                .anySatisfy(e -> {
                    assertThat(e.getAction()).isEqualTo("health-calc");
                    assertThat(e.getObject()).isEqualTo("hc-batch-1");
                    assertThat(e.getDetail()).contains("ruleVersion=v1");
                });
    }

    @Test
    void reingestNewBatchRecomputesAndIncrementsRuleVersion() throws Exception {
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-v1", "asset-1", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        // 重新接入新批次 → 重算 → ruleVersion 递增
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-v2", "asset-1", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dq/health/{assetId}", "asset-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleVersion").value("v2"))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.state").value("ok"));
    }

    @Test
    void expiredBatchDerivesExpiredDisplayStateDistinctFromNoresult() throws Exception {
        // 执行时间 30+ 天前 → validUntil 已过 → 过期独立展示态
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-expired", "asset-expired", EXPIRED_EXECUTION_TIME,
                                allPassedRules())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dq/health").param("assetId", "asset-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].state").value("expired"))
                .andExpect(jsonPath("$.data[0].expired").value(true))
                .andExpect(jsonPath("$.data[0].band").doesNotExist())
                .andExpect(jsonPath("$.data[0].score").value(100))
                .andExpect(jsonPath("$.data[0].hasResult").value(true))
                .andExpect(jsonPath("$.data[0].validUntil").isNotEmpty());

        // 过期与无结果独立展示态不混淆：band=noresult 筛选命中恒空（无结果不落健康分表）
        mockMvc.perform(get("/api/dq/health").param("band", "noresult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0));

        // band=expired 筛选命中过期资产
        mockMvc.perform(get("/api/dq/health").param("band", "expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].assetId").value("asset-expired"));
    }

    @Test
    void detailReturnsFieldLevelHealthWithLowScoreFlag() throws Exception {
        String rules = passedRule("", "非空率", "non-null-rate") + "," + passedRule("", "格式", "format")
                + "," + passedRule("", "唯一性", "uniqueness") + "," + passedRule("", "值域", "value-range")
                + "," + passedRule("", "新鲜度", "freshness")
                + "," + passedRule("name", "非空率-name", "non-null-rate")
                + "," + passedRule("name", "格式-name", "format")
                + "," + passedRule("name", "唯一性-name", "uniqueness")
                + "," + passedRule("name", "值域-name", "value-range")
                + "," + passedRule("name", "新鲜度-name", "freshness")
                + "," + failedRule("amount", "非空率-amount", "non-null-rate");
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-fields", "asset-fields", EXECUTION_TIME, rules)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dq/health/{assetId}", "asset-fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("asset-fields"))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.band").value("优"))
                .andExpect(jsonPath("$.data.ruleVersion").value("v1"))
                .andExpect(jsonPath("$.data.sourceTool").value("great-expectations"))
                .andExpect(jsonPath("$.data.passRate").value("100%"))
                .andExpect(jsonPath("$.data.fields.length()").value(2))
                .andExpect(jsonPath("$.data.fields[?(@.fieldName=='name')].lowScore").value(false))
                .andExpect(jsonPath("$.data.fields[?(@.fieldName=='name')].band").value("优"))
                .andExpect(jsonPath("$.data.fields[?(@.fieldName=='amount')].lowScore").value(true))
                .andExpect(jsonPath("$.data.fields[?(@.fieldName=='amount')].band").value("差"));
    }

    @Test
    void ruleDetailReturnsScoreSourceAreaAndRuleRows() throws Exception {
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-detail", "asset-1", EXECUTION_TIME, allPassedRules())))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/dq/health/{assetId}/details", "asset-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("asset-1"))
                .andExpect(jsonPath("$.data.ruleVersion").value("v1"))
                .andExpect(jsonPath("$.data.batchNo").value("hc-batch-detail"))
                .andExpect(jsonPath("$.data.expired").value(false))
                .andExpect(jsonPath("$.data.algorithm.formula").value(
                        "健康分 = Σ(规则权重 × 规则得分)，规则得分 passed=100 / warn=80 / failed|error=0"))
                .andExpect(jsonPath("$.data.algorithm.weights.length()").value(5))
                .andExpect(jsonPath("$.data.rules.length()").value(5))
                .andExpect(jsonPath("$.data.rules[0].weight").isNotEmpty())
                .andExpect(jsonPath("$.data.rules[0].toolTime").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        double weightSum = 0;
        for (int i = 0; i < 5; i++) {
            weightSum += objectMapper.readTree(body).path("data").path("algorithm").path("weights")
                    .get(i).path("weight").asDouble();
        }
        // 权重合计为 1（C22 公信力关键）
        assertThat(weightSum).isEqualTo(1.0d);
    }

    @Test
    void ruleDetailSupportsFieldFilter() throws Exception {
        String rules = passedRule("", "非空率", "non-null-rate")
                + "," + passedRule("name", "非空率-name", "non-null-rate");
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson("hc-batch-field-detail", "asset-fields", EXECUTION_TIME, rules)))
                .andExpect(status().isCreated());

        // 字段级过滤：仅返回该字段规则
        mockMvc.perform(get("/api/dq/health/{assetId}/details", "asset-fields")
                        .param("fieldName", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldName").value("name"))
                .andExpect(jsonPath("$.data.rules.length()").value(1))
                .andExpect(jsonPath("$.data.rules[0].fieldName").value("name"));

        // 资产级（无 fieldName 参数）
        mockMvc.perform(get("/api/dq/health/{assetId}/details", "asset-fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldName").doesNotExist())
                .andExpect(jsonPath("$.data.rules.length()").value(1));
    }

    @Test
    void healthListSupportsPaginationDomainBandAndAssetIdFilters() throws Exception {
        ingest("hc-list-1", "asset-1", EXECUTION_TIME, allPassedRules());
        ingest("hc-list-2", "asset-2", EXECUTION_TIME, allPassedRules());
        ingest("hc-list-3", "asset-3", EXECUTION_TIME, allPassedRules());

        mockMvc.perform(get("/api/dq/health").param("domain", "交易域"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(2));

        mockMvc.perform(get("/api/dq/health").param("band", "优"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/dq/health").param("assetId", "asset-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].assetId").value("asset-2"));

        mockMvc.perform(get("/api/dq/health").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(2));
    }

    @Test
    void emptyHealthListReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/dq/health").param("domain", "不存在的域"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void unknownAssetHealthReturns404NotFound() throws Exception {
        mockMvc.perform(get("/api/dq/health/{assetId}", "no-such-asset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("err.dq.not-found"));

        mockMvc.perform(get("/api/dq/health/{assetId}/details", "no-such-asset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("err.dq.not-found"));
    }

    @Test
    void expiredDetailsCarriesExpiredFlagAndValidUntil() throws Exception {
        ingest("hc-batch-expired-2", "asset-expired", EXPIRED_EXECUTION_TIME, allPassedRules());

        mockMvc.perform(get("/api/dq/health/{assetId}/details", "asset-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("expired"))
                .andExpect(jsonPath("$.data.expired").value(true))
                .andExpect(jsonPath("$.data.band").doesNotExist())
                .andExpect(jsonPath("$.data.validUntil").isNotEmpty());
    }

    private void ingest(String batchNo, String assetId, String executionTime, String rules) throws Exception {
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(geJson(batchNo, assetId, executionTime, rules)))
                .andExpect(status().isCreated());
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

    private static String failedRule(String fieldName, String ruleName, String ruleType) {
        return "{\"ruleName\":\"" + ruleName + "\",\"ruleType\":\"" + ruleType
                + "\",\"status\":\"failed\"" + fieldJson(fieldName) + "}";
    }

    private static String fieldJson(String fieldName) {
        return (fieldName == null || fieldName.isEmpty()) ? "" : ",\"fieldName\":\"" + fieldName + "\"";
    }

    private static String geJson(String batchNo, String assetId, String executionTime, String rules) {
        return "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"" + batchNo + "\","
                + "\"executionTime\":\"" + executionTime + "\",\"channelId\":\"ch-1\","
                + "\"assetId\":\"" + assetId + "\",\"results\":[" + rules + "]}";
    }
}
