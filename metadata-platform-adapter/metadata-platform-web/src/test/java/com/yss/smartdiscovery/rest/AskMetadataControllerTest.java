package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.AskResponseDTO;
import com.yss.smartdiscovery.application.dto.RecommendedAssetDTO;
import com.yss.smartdiscovery.application.service.AskMetadataAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class AskMetadataControllerTest {

    private AskMetadataAppService appService;
    private AskMetadataController controller;

    @BeforeEach
    void setUp() {
        appService = Mockito.mock(AskMetadataAppService.class);
        controller = new AskMetadataController(appService);
    }

    @Test
    @DisplayName("POST /ask - 自然语言意图找数与质量分联动")
    void testAskMetadata() {
        AskResponseDTO mockResponse = AskResponseDTO.builder()
                .intentSummary("已提取意图")
                .extractedEntities(Arrays.asList("客户", "交易"))
                .recommendedAssets(Collections.singletonList(
                        RecommendedAssetDTO.builder()
                                .tableName("ads_vip_trade_east_china_di")
                                .matchScore(98)
                                .dqHealthScore(96)
                                .dqLevel("优")
                                .build()
                ))
                .build();
        Mockito.when(appService.askMetadata(anyString())).thenReturn(mockResponse);

        SingleResult<AskResponseDTO> res = controller.askMetadata(Collections.singletonMap("queryText", "查找近半年华东高净值客户交易表"));
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().getRecommendedAssets()).hasSize(1);
        assertThat(res.getData().getRecommendedAssets().get(0).getDqHealthScore()).isEqualTo(96);
    }
}
