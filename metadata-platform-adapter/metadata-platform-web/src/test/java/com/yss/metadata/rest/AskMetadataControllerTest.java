package com.yss.metadata.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.metadata.application.ai.AskMetadataApplicationService;
import com.yss.metadata.application.ai.convertor.AskMetadataConvertor;
import com.yss.metadata.client.dto.cmd.AskMetadataCmd;
import com.yss.metadata.domain.ai.gateway.AskMetadataLogGateway;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.infrastructure.ai.LlmClientGatewayImpl;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.asset.support.InMemorySearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 智能找数 REST 契约与接口测试（POST /api/ai/ask-metadata）
 *
 * @author ai
 * @since 2026-08-15
 */
class AskMetadataControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        assetRepository.seedSourceName("src-01", "MySQL-Trade");
        InMemorySearchIndex searchIndex = new InMemorySearchIndex(assetRepository);

        Asset asset = Asset.builder()
                .id("ast-101")
                .sourceId("src-01")
                .name("dwd_trade_order_di")
                .type("table")
                .domain("trade")
                .status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.now())
                .build();

        assetRepository.save(asset);


        LlmClientGatewayImpl llmGateway = new LlmClientGatewayImpl();
        AskMetadataLogGateway logGateway = (id, userId, queryText, matchedAssetIds, confidenceScore, modelName, createdAt) -> {};

        AskMetadataApplicationService service = new AskMetadataApplicationService(llmGateway, searchIndex, logGateway);
        AskMetadataController controller = new AskMetadataController(service, org.mapstruct.factory.Mappers.getMapper(AskMetadataConvertor.class));


        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/ai/ask-metadata 成功返回 200 与匹配资产卡片")
    void testAskMetadataSuccess() throws Exception {
        AskMetadataCmd cmd = AskMetadataCmd.builder()
                .query("查询公募交易表")
                .domain("trade")
                .limit(5)
                .build();

        mockMvc.perform(post("/api/ai/ask-metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.query").value("查询公募交易表"))
                .andExpect(jsonPath("$.data.matchedAssets", hasSize(1)))
                .andExpect(jsonPath("$.data.matchedAssets[0].assetName").value("dwd_trade_order_di"));

    }

    @Test
    @DisplayName("POST /api/ai/ask-metadata 参数校验失败返回 422")
    void testAskMetadataValidationFail() throws Exception {
        AskMetadataCmd cmd = AskMetadataCmd.builder()
                .query("") // 空查询
                .build();

        mockMvc.perform(post("/api/ai/ask-metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"));
    }
}
