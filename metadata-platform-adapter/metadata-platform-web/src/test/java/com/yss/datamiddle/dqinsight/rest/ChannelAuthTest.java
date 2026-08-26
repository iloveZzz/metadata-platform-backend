package com.yss.datamiddle.dqinsight.rest;

import com.yss.datamiddle.dqinsight.DqInsightTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.rest.filter.TokenFingerprint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通道级 Token 认证中间件测试（WU4 / B1，securitySchemes ChannelTokenAuth 回写冻结 YAML）。
 *
 * <p>认证失败 → 422 err.dq.auth.invalid（错误分类 auth 一致）且错误信息脱敏（C19，不泄露凭证）；
 * 每通道独立 AK/SK（SB-09 基线）；凭证存储（dq_channel 表）由切片 04 落地，本用例以端口 + 测试 fixture
 * 验证认证与脱敏逻辑（合同 seam_deferred）。</p>
 */
@SpringBootTest(classes = DqInsightTestApplication.class)
@AutoConfigureMockMvc
@Transactional
class ChannelAuthTest {

    private static final String SECRET_TOKEN = "super-secret-channel-token-xyz";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChannelCredentialStore channelCredentialStore;

    @MockBean
    private CatalogAclGateway catalogAclGateway;

    @Test
    void postWithoutTokenReturns422AuthInvalid() throws Exception {
        mockMvc.perform(post("/api/dq/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.auth.invalid"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("Authorization"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("err.dq.auth.invalid"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(SECRET_TOKEN))));
    }

    @Test
    void postWithInvalidTokenReturns422AuthInvalidDesensitized() throws Exception {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex(SECRET_TOKEN)))
                .thenReturn(Optional.empty());

        String responseBody = mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", SECRET_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("err.dq.auth.invalid"))
                .andReturn().getResponse().getContentAsString();

        // 脱敏：错误响应与日志均不得回显凭证
        org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain(SECRET_TOKEN);
    }

    @Test
    void postWithValidTokenPassesAuthenticationAndIngests() throws Exception {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex("valid-token-1")))
                .thenReturn(Optional.of(new ChannelCredential("ch-9", "ak-9", true)));
        when(catalogAclGateway.lookupAsset("asset-auth")).thenReturn(AssetLookupResult.notFound("asset-auth"));

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", "Bearer valid-token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("asset-auth")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ingested"));
    }

    @Test
    void postWithBearerPrefixTokenIsAccepted() throws Exception {
        when(channelCredentialStore.findByTokenFingerprint(TokenFingerprint.sha256Hex("bearer-only-token")))
                .thenReturn(Optional.of(new ChannelCredential("ch-b", "ak-b", true)));
        when(catalogAclGateway.lookupAsset("asset-bearer"))
                .thenReturn(AssetLookupResult.notFound("asset-bearer"));

        mockMvc.perform(post("/api/dq/results")
                        .header("Authorization", "Bearer bearer-only-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("asset-bearer")))
                .andExpect(status().isCreated());
    }

    @Test
    void getDoesNotRequireChannelAuth() throws Exception {
        // GET /api/dq/results 未施加 security（冻结 YAML 仅 POST 声明 ChannelTokenAuth）
        mockMvc.perform(get("/api/dq/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private String validJson() {
        return validJson("asset-auth");
    }

    private String validJson(String assetId) {
        return "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"auth-batch\","
                + "\"executionTime\":\"2026-08-11T10:00:00Z\",\"assetId\":\"" + assetId
                + "\",\"results\":[{\"ruleName\":\"r\",\"ruleType\":\"format\",\"status\":\"passed\"}]}";
    }
}
