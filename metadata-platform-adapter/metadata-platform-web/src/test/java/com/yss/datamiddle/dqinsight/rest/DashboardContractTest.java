package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.rest.filter.TokenFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 仪表盘聚合契约测试（DQI-SLICE-03-WU1，冻结 OpenAPI dq-dashboard）。
 *
 * <p>覆盖：bandDistribution（优 / 良 / 差 + 过期独立展示态）/ 已接入（含过期）/ 低分 / 覆盖率
 * （SB-07）/ 资产列表分页筛选排序 / 空分页 0 条 / DataDomainFilter seam 域外不展示（C24）。
 * 信封形态 A3-AM-01：stats 在 data 内，assets 为嵌套 PageResult（data 为数组 + totalCount）。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class DashboardContractTest {

    private static final String VALID_TOKEN = "channel-token-dash-003";
    private static final String BEARER_VALID = "Bearer " + VALID_TOKEN;
    private static final String EXECUTION_TIME = "2026-08-11T10:00:00+08:00";
    private static final String EXPIRED_EXECUTION_TIME = "2026-06-01T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private ChannelCredentialStore channelCredentialStore;

    @MockBean
    private DataDomainFilter dataDomainFilter;

    @BeforeEach
    void setUp() {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(VALID_TOKEN)))
                .thenReturn(Optional.of(new ChannelCredential("ch-1", "ak-1", true)));
        when(catalogAclGateway.countVisibleTargetAssets(any())).thenReturn(5);
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.emptyList());
        when(catalogAclGateway.lookupAsset("asset-1")).thenReturn(AssetLookupResult.found("asset-1",
                AssetSnapshot.builder().assetId("asset-1").assetName("alpha").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-2")).thenReturn(AssetLookupResult.found("asset-2",
                AssetSnapshot.builder().assetId("asset-2").assetName("beta").domain("交易域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-3")).thenReturn(AssetLookupResult.found("asset-3",
                AssetSnapshot.builder().assetId("asset-3").assetName("gamma").domain("风控域")
                        .assetType("table").build()));
        when(catalogAclGateway.lookupAsset("asset-exp")).thenReturn(AssetLookupResult.found("asset-exp",
                AssetSnapshot.builder().assetId("asset-exp").assetName("delta").domain("交易域")
                        .assetType("table").build()));
    }

    @Test
    void dashboardReturnsStatsAndPaginatedAssetRows() throws Exception {
        ingest("dash-batch-1", "asset-1", EXECUTION_TIME, allPassedRules());
        ingest("dash-batch-2", "asset-2", EXECUTION_TIME, allPassedRules());
        ingest("dash-batch-3", "asset-3", EXECUTION_TIME, allFailedRules());

        mockMvc.perform(get("/api/dq/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // stats（数据域内可见资产全集；target=5 mock）
                .andExpect(jsonPath("$.data.stats.bandDistribution.good").value(2))
                .andExpect(jsonPath("$.data.stats.bandDistribution.fair").value(0))
                .andExpect(jsonPath("$.data.stats.bandDistribution.poor").value(1))
                .andExpect(jsonPath("$.data.stats.bandDistribution.expired").value(0))
                .andExpect(jsonPath("$.data.stats.bandDistribution.noResult").value(2))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(3))
                .andExpect(jsonPath("$.data.stats.lowScoreAssetCount").value(1))
                .andExpect(jsonPath("$.data.stats.targetAssetCount").value(5))
                // 覆盖率 = 3 ÷ 5 = 60（SB-07）
                .andExpect(jsonPath("$.data.stats.coverage").value(60.0))
                // assets 嵌套 PageResult（A3-AM-01 信封）
                .andExpect(jsonPath("$.data.assets.data.length()").value(3))
                .andExpect(jsonPath("$.data.assets.totalCount").value(3))
                .andExpect(jsonPath("$.data.assets.pageIndex").value(1))
                .andExpect(jsonPath("$.data.assets.pageSize").value(20))
                .andExpect(jsonPath("$.data.assets.data[0].band").isNotEmpty());
    }

    @Test
    void dashboardListSortsByScoreDescendingAndNameAscending() throws Exception {
        ingest("dash-sort-1", "asset-1", EXECUTION_TIME, allPassedRules());   // alpha 100 优
        ingest("dash-sort-2", "asset-2", EXECUTION_TIME, allFailedRules());   // beta 0 差

        // sort=score → 高分优先（降序）
        mockMvc.perform(get("/api/dq/dashboard").param("sort", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets.data[0].assetId").value("asset-1"))
                .andExpect(jsonPath("$.data.assets.data[0].score").value(100))
                .andExpect(jsonPath("$.data.assets.data[1].assetId").value("asset-2"));

        // sort=name → 名称升序（alpha < beta）
        mockMvc.perform(get("/api/dq/dashboard").param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets.data[0].assetId").value("asset-1"))
                .andExpect(jsonPath("$.data.assets.data[1].assetId").value("asset-2"));
    }

    @Test
    void dashboardBandFilterAppliesToListOnly() throws Exception {
        ingest("dash-band-1", "asset-1", EXECUTION_TIME, allPassedRules());
        ingest("dash-band-2", "asset-3", EXECUTION_TIME, allFailedRules());

        // band=优：列表仅优档；stats 仍为全集口径（档位筛选不作用于统计）
        mockMvc.perform(get("/api/dq/dashboard").param("band", "优"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.bandDistribution.good").value(1))
                .andExpect(jsonPath("$.data.stats.bandDistribution.poor").value(1))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(2))
                .andExpect(jsonPath("$.data.assets.data.length()").value(1))
                .andExpect(jsonPath("$.data.assets.data[0].assetId").value("asset-1"))
                .andExpect(jsonPath("$.data.assets.totalCount").value(1));
    }

    @Test
    void dashboardExpiredCountsIntoIngestedWithExpiredDistribution() throws Exception {
        ingest("dash-exp-1", "asset-exp", EXPIRED_EXECUTION_TIME, allPassedRules());

        mockMvc.perform(get("/api/dq/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.bandDistribution.good").value(0))
                .andExpect(jsonPath("$.data.stats.bandDistribution.expired").value(1))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(1)) // 过期计入已接入
                .andExpect(jsonPath("$.data.stats.lowScoreAssetCount").value(0))
                .andExpect(jsonPath("$.data.stats.coverage").value(20.0)) // 1 ÷ 5
                .andExpect(jsonPath("$.data.assets.data[0].state").value("expired"))
                .andExpect(jsonPath("$.data.assets.data[0].expired").value(true))
                .andExpect(jsonPath("$.data.assets.data[0].band").doesNotExist());
    }

    @Test
    void dashboardAppliesVisibleDomainSeamExcludingOutOfDomain() throws Exception {
        ingest("dash-rbac-1", "asset-1", EXECUTION_TIME, allPassedRules()); // 交易域
        ingest("dash-rbac-2", "asset-3", EXECUTION_TIME, allPassedRules()); // 风控域
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.singletonList("交易域"));

        mockMvc.perform(get("/api/dq/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.bandDistribution.good").value(1))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(1)) // 风控域不展示（C24）
                .andExpect(jsonPath("$.data.assets.data.length()").value(1))
                .andExpect(jsonPath("$.data.assets.data[0].assetId").value("asset-1"))
                .andExpect(jsonPath("$.data.assets.totalCount").value(1));
    }

    @Test
    void dashboardNoDataReturnsEmptyPageNotError() throws Exception {
        mockMvc.perform(get("/api/dq/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stats.bandDistribution.good").value(0))
                .andExpect(jsonPath("$.data.stats.bandDistribution.noResult").value(5))
                .andExpect(jsonPath("$.data.stats.ingestedAssetCount").value(0))
                .andExpect(jsonPath("$.data.stats.coverage").value(0.0))
                .andExpect(jsonPath("$.data.assets.data").isArray())
                .andExpect(jsonPath("$.data.assets.data.length()").value(0))
                .andExpect(jsonPath("$.data.assets.totalCount").value(0));
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

    private static String allFailedRules() {
        return failedRule("", "非空率", "non-null-rate") + "," + failedRule("", "格式", "format")
                + "," + failedRule("", "唯一性", "uniqueness") + "," + failedRule("", "值域", "value-range")
                + "," + failedRule("", "新鲜度", "freshness");
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
