package com.yss.datamiddle.dqinsight.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreCalculationTrigger;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.rest.filter.TokenFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接入契约测试（WU3 + WU1/WU2 端到端）：201 / 409 / 422 / 413、CSV 行级 fieldErrors、
 * 幂等去重、资产未命中 pending 不阻断入库、空分页。
 *
 * <p>POST /api/dq/results 前置通道级 Token 认证（B1）；测试以 fixture 凭证指纹通过认证。
 * 用例级事务回滚保证批次隔离。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class ResultsContractTest {

    private static final String VALID_TOKEN = "channel-token-valid-001";
    private static final String BEARER_VALID = "Bearer " + VALID_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @MockBean
    private ChannelCredentialStore channelCredentialStore;

    @MockBean
    private HealthScoreCalculationTrigger healthScoreCalculationTrigger;

    @BeforeEach
    void setUp() {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(VALID_TOKEN)))
                .thenReturn(Optional.of(new ChannelCredential("ch-1", "ak-1", true)));
    }

    @Test
    void submitGeJsonIngestsBatchWithLinkedStatus201() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-1")).thenReturn(AssetLookupResult.found("asset-1",
                AssetSnapshot.builder().assetId("asset-1").assetName("用户表").domain("交易域")
                        .assetType("table").build()));
        String json = geJson("ge-batch-201", "asset-1");

        MvcResult result = mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchNo").value("ge-batch-201"))
                .andExpect(jsonPath("$.data.status").value("ingested"))
                .andExpect(jsonPath("$.data.linkageStatus").value("linked"))
                .andExpect(jsonPath("$.data.rowCount").value(1))
                .andExpect(jsonPath("$.data.batchId").isNotEmpty())
                .andReturn();

        JsonNode receipt = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String batchId = receipt.path("batchId").asText();
        assertThat(batchId).isNotBlank();

        // 关联命中 → 健康分计算触发（切片 02 seam）
        verify(healthScoreCalculationTrigger).triggerForAssets(eq(batchId), any());
    }

    @Test
    void submitCsvIngestsBatch201() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-c1")).thenReturn(AssetLookupResult.notFound("asset-c1"));
        String csv = "asset_id,field_name,rule_name,rule_type,status,failure_reason,execution_time,batch_no\n"
                + "asset-c1,,非空率,non-null-rate,passed,,2026-08-11T10:00:00+08:00,csv-batch-1\n";

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", BEARER_VALID)
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batchNo").value("csv-batch-1"))
                .andExpect(jsonPath("$.data.status").value("ingested"))
                .andExpect(jsonPath("$.data.linkageStatus").value("pending"))
                .andExpect(jsonPath("$.data.rowCount").value(1));
    }

    @Test
    void assetNotFoundStillIngestsWithPendingLinkage201() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-missing")).thenReturn(AssetLookupResult.notFound("asset-missing"));
        String json = geJson("ge-batch-pending", "asset-missing");

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.linkageStatus").value("pending"));
    }

    @Test
    void assetNetworkFailureReturns422NetworkTimeout() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-net"))
                .thenReturn(AssetLookupResult.networkFailure("asset-net"));
        String json = geJson("ge-batch-net", "asset-net");

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.network.timeout"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assetId"));

        verify(healthScoreCalculationTrigger, never()).triggerForAssets(any(), any());
    }

    @Test
    void duplicateBatchReturns409WithoutReIngestion() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-1")).thenReturn(AssetLookupResult.notFound("asset-1"));
        String json = geJson("ge-batch-dup", "asset-1");

        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("err.dq.batch.duplicate"));

        // 不重复入库：用例事务隔离下 GET 仅 1 条
        mockMvc.perform(get("/api/dq/results")
                        .param("sourceTool", "great-expectations")
                        .param("linkageStatus", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void csvSchemaViolationReturns422WithRowLevelFieldErrors() throws Exception {
        String csv = "asset_id,field_name,rule_name,rule_type,status,failure_reason,execution_time,batch_no\n"
                + "asset-c1,,r1,non-null-rate,passed,,2026-08-11T10:00:00+08:00,bad-csv\n"
                + "asset-c2,,r2,bad-rule-type,bad-status,,2026-08-11T10:00:00+08:00,bad-csv\n";

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", BEARER_VALID)
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.csv.schema"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("row:3.rule_type"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("err.dq.csv.schema"));
    }

    @Test
    void parseFailedBatchIsQueryableByStatusFilter() throws Exception {
        String csv = "asset_id,field_name,rule_name,rule_type,status,failure_reason,execution_time,batch_no\n"
                + "asset-c1,,r1,bad-rule-type,passed,,2026-08-11T10:00:00+08:00,parse-fail-record\n";

        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType("text/csv").content(csv))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/dq/results").param("status", "parse-failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].batchNo").value("parse-fail-record"))
                .andExpect(jsonPath("$.data[0].errorCategory").value("format"));
    }

    @Test
    void jsonMissingRequiredFieldsReturns422FormatInvalid() throws Exception {
        String json = "{\"sourceTool\":\"great-expectations\",\"results\":[]}";

        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.format.invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").isNotEmpty());
    }

    @Test
    void batchOverLimitReturns413TooLarge() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"sourceTool\":\"great-expectations\",\"batchNo\":\"huge-batch\",")
                .append("\"executionTime\":\"2026-08-11T10:00:00Z\",\"assetId\":\"asset-1\",\"results\":[");
        for (int i = 0; i < 50_001; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"ruleName\":\"r").append(i).append("\",\"ruleType\":\"format\",\"status\":\"passed\"}");
        }
        sb.append("]}");

        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(sb.toString()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("err.dq.batch.too-large"));
    }

    @Test
    void getResultsSupportsFiltersAndPagination() throws Exception {
        when(catalogAclGateway.lookupAsset("asset-1")).thenReturn(AssetLookupResult.notFound("asset-1"));
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(geJson("q-1", "asset-1")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/dq/results").header("Authorization", BEARER_VALID)
                        .contentType(MediaType.APPLICATION_JSON).content(geJson("q-2", "asset-1")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dq/results")
                        .param("sourceTool", "great-expectations")
                        .param("linkageStatus", "pending")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(1));
    }

    @Test
    void getResultsEmptyPageReturnsZeroTotal() throws Exception {
        mockMvc.perform(get("/api/dq/results").param("channelId", "no-such-channel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    private String geJson(String batchNo, String assetId) {
        return "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"" + batchNo + "\","
                + "\"executionTime\":\"2026-08-11T10:00:00+08:00\",\"channelId\":\"ch-1\","
                + "\"assetId\":\"" + assetId + "\",\"results\":[{\"fieldName\":\"name\","
                + "\"ruleName\":\"非空率\",\"ruleType\":\"non-null-rate\",\"status\":\"passed\"}]}";
    }
}
