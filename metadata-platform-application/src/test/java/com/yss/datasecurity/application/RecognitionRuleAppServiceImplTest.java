package com.yss.datasecurity.application;

import com.yss.datasecurity.application.convertor.RecognitionRuleConvertor;
import com.yss.datasecurity.application.dto.RecognitionRuleBatchRunDTO;
import com.yss.datasecurity.application.service.impl.RecognitionRuleAppServiceImpl;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.RecognitionRuleGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecognitionRuleAppServiceImplTest {

    @Mock
    private RecognitionRuleGateway recognitionRuleGateway;

    @Mock
    private RecognitionRuleConvertor convertor;

    @Mock
    private SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;

    @Mock
    private DataCategoryGateway dataCategoryGateway;

    @Mock
    private SecurityGradeGateway securityGradeGateway;

    @InjectMocks
    private RecognitionRuleAppServiceImpl recognitionRuleAppService;

    private DataCategory securitiesCategory;
    private SecurityGrade l4Grade;
    private RecognitionRule securitiesScanRule;

    @BeforeEach
    void setUp() {
        l4Grade = SecurityGrade.builder()
                .id(4L)
                .gradeName("高度敏感数据 (L4)")
                .gradeCode("L4")
                .sensitivityScore(85)
                .colorTag("volcano")
                .build();

        securitiesCategory = DataCategory.builder()
                .id(10004L)
                .categoryName("证券资金账户")
                .categoryCode("CAT_SECURITY_ACC")
                .treeNodeId(1004L)
                .securityGradeId(4L)
                .securityGradeName("高度敏感数据 (L4)")
                .sensitivityScore(85)
                .status("ENABLED")
                .build();

        securitiesScanRule = RecognitionRule.builder()
                .id(3001L)
                .ruleName("证券资金扫描")
                .description("自动扫描并识别证券资金交易事实表中的核心资金账号与交易要素")
                .priority(15)
                .owner("admin")
                .status("ENABLED")
                .categoryScopeMode("SPECIFIC")
                .categoryScopeConfig("[{\"treeNodeId\":1004,\"categoryIds\":[10004]}]")
                .scanSourceType("DATASOURCE")
                .datasourceScopeConfig("{\"datasourceIds\":[\"dataphin_fashion_cdm\"],\"tableScopeType\":\"SPECIFIC_TABLES\"}")
                .lineageInheritanceEnabled(true)
                .taggedFieldsCount(0)
                .build();
    }

    @Test
    @DisplayName("测试证券资金交易事实表样本数据识别与规则扫描打标")
    void testSecuritiesFundAccountScanAndTagging() {
        when(recognitionRuleGateway.findById(3001L)).thenReturn(Optional.of(securitiesScanRule));
        when(dataCategoryGateway.listAll(null, null, "ENABLED")).thenReturn(Collections.singletonList(securitiesCategory));
        when(securityGradeGateway.findById(4L)).thenReturn(Optional.of(l4Grade));
        when(sensitiveTaggingRecordGateway.findByTableAndField(any(), any(), any())).thenReturn(Optional.empty());
        when(sensitiveTaggingRecordGateway.countByMatchedRuleId(3001L)).thenReturn(1L);

        RecognitionRuleBatchRunDTO batchRunDTO = RecognitionRuleBatchRunDTO.builder()
                .ruleIds(Collections.singletonList(3001L))
                .ruleScope("ENABLED_ONLY")
                .lineageInheritanceEnabled(true)
                .build();

        int matchedCount = recognitionRuleAppService.batchRun(batchRunDTO);

        assertTrue(matchedCount >= 1, "应至少命中并打标1个敏感字段");

        ArgumentCaptor<SensitiveTaggingRecord> recordCaptor = ArgumentCaptor.forClass(SensitiveTaggingRecord.class);
        verify(sensitiveTaggingRecordGateway, atLeastOnce()).save(recordCaptor.capture());

        List<SensitiveTaggingRecord> savedRecords = recordCaptor.getAllValues();
        SensitiveTaggingRecord fundAccRecord = savedRecords.stream()
                .filter(r -> "fund_acc_no".equals(r.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(fundAccRecord, "应成功识别出 fund_acc_no 字段");
        assertEquals("fct_sec_fund_trans_di", fundAccRecord.getTableName());
        assertEquals("证券资金账户", fundAccRecord.getCategoryName());
        assertEquals(10004L, fundAccRecord.getCategoryId());
        assertEquals("高度敏感数据 (L4)", fundAccRecord.getSecurityGradeName());
        assertEquals("621088********7465", fundAccRecord.getSamplePreview());
        assertEquals(98.5, fundAccRecord.getConfidenceScore());
        assertEquals("ENABLED", fundAccRecord.getMaskingStatus());
        assertEquals("AUTO", fundAccRecord.getRecognitionMethod());
    }
}
