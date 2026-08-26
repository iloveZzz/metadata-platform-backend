package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.RecognitionRuleConvertor;
import com.yss.datasecurity.application.convertor.RecognitionRuleConvertorImpl;
import com.yss.datasecurity.application.dto.RecognitionRuleBatchRunDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleManualScanDTO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecognitionRuleExecutionTest {

    @Mock
    private RecognitionRuleGateway recognitionRuleGateway;

    @Mock
    private SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;

    @Mock
    private DataCategoryGateway dataCategoryGateway;

    @Mock
    private SecurityGradeGateway securityGradeGateway;

    private RecognitionRuleConvertor convertor = new RecognitionRuleConvertorImpl();

    private RecognitionRuleAppServiceImpl appService;

    private RecognitionRule rule1;
    private DataCategory catIdCard;
    private DataCategory catMobilePhone;
    private SecurityGrade gradeL4;
    private SecurityGrade gradeL3;

    @BeforeEach
    void setUp() {
        appService = new RecognitionRuleAppServiceImpl(
                recognitionRuleGateway,
                convertor,
                sensitiveTaggingRecordGateway,
                dataCategoryGateway,
                securityGradeGateway
        );

        rule1 = RecognitionRule.builder()
                .id(2001L)
                .ruleName("客户身份识别")
                .status("ENABLED")
                .categoryScopeMode("ALL")
                .scanSourceType("DATAPHIN")
                .priority(10)
                .taggedFieldsCount(0)
                .build();

        catIdCard = DataCategory.builder()
                .id(1011L)
                .categoryName("居民身份证(中国大陆)")
                .securityGradeId(4L)
                .securityGradeName("L4")
                .sensitivityScore(4)
                .build();

        catMobilePhone = DataCategory.builder()
                .id(1012L)
                .categoryName("移动电话")
                .securityGradeId(3L)
                .securityGradeName("L3")
                .sensitivityScore(3)
                .build();

        gradeL4 = SecurityGrade.builder()
                .id(4L)
                .gradeName("L4")
                .sensitivityScore(4)
                .build();

        gradeL3 = SecurityGrade.builder()
                .id(3L)
                .gradeName("L3")
                .sensitivityScore(3)
                .build();
    }

    @Test
    @DisplayName("测试 batchRun 首次运行规则产生识别结果并持久化入库")
    void testBatchRun_FirstScan_CreatesNewSensitiveRecords() {
        when(recognitionRuleGateway.findById(2001L)).thenReturn(Optional.of(rule1));
        when(dataCategoryGateway.listAll(null, null, "ENABLED")).thenReturn(Arrays.asList(catIdCard, catMobilePhone));
        when(securityGradeGateway.findById(4L)).thenReturn(Optional.of(gradeL4));
        when(securityGradeGateway.findById(3L)).thenReturn(Optional.of(gradeL3));
        when(sensitiveTaggingRecordGateway.findByTableAndField(any(), any(), any())).thenReturn(Optional.empty());
        when(sensitiveTaggingRecordGateway.countByMatchedRuleId(2001L)).thenReturn(2L);

        RecognitionRuleBatchRunDTO dto = RecognitionRuleBatchRunDTO.builder()
                .ruleIds(Collections.singletonList(2001L))
                .build();

        int resultCount = appService.batchRun(dto);

        assertTrue(resultCount >= 2, "应至少扫描并匹配命中 2 个敏感字段");

        ArgumentCaptor<SensitiveTaggingRecord> recordCaptor = ArgumentCaptor.forClass(SensitiveTaggingRecord.class);
        verify(sensitiveTaggingRecordGateway, atLeastOnce()).save(recordCaptor.capture());

        List<SensitiveTaggingRecord> savedRecords = recordCaptor.getAllValues();
        assertFalse(savedRecords.isEmpty());

        SensitiveTaggingRecord idCardRecord = savedRecords.stream()
                .filter(r -> "id_card_num".equals(r.getFieldName()))
                .findFirst().orElse(null);

        assertNotNull(idCardRecord);
        assertEquals("居民身份证(中国大陆)", idCardRecord.getCategoryName());
        assertEquals("L4", idCardRecord.getSecurityGradeName());
        assertEquals("ENABLED", idCardRecord.getMaskingStatus());
        assertEquals("110101********2345", idCardRecord.getSamplePreview());
        assertEquals("AUTO", idCardRecord.getRecognitionMethod());
        assertFalse(idCardRecord.getIsLocked());

        verify(recognitionRuleGateway, times(1)).update(rule1);
        assertEquals(2, rule1.getTaggedFieldsCount());
    }

    @Test
    @DisplayName("测试已锁定记录在规则运行时不被覆盖，而是生成更优推荐")
    void testRuleRun_WithLockedExistingRecord_GeneratesRecommendation() {
        when(recognitionRuleGateway.listAllActiveRules()).thenReturn(Collections.singletonList(rule1));
        when(dataCategoryGateway.listAll(null, null, "ENABLED")).thenReturn(Arrays.asList(catIdCard, catMobilePhone));
        when(securityGradeGateway.findById(4L)).thenReturn(Optional.of(gradeL4));
        when(securityGradeGateway.findById(3L)).thenReturn(Optional.of(gradeL3));

        SensitiveTaggingRecord existingLockedRecord = SensitiveTaggingRecord.builder()
                .id(3001L)
                .datasourceId("dataphin_fashion_ods")
                .tableName("ods_hzct_user_info")
                .fieldName("id_card_num")
                .categoryId(9999L)
                .categoryName("旧自定义分类")
                .securityGradeId(1L)
                .securityGradeName("L1")
                .sensitivityScore(1)
                .isLocked(true)
                .hasBetterRecommendation(false)
                .build();

        when(sensitiveTaggingRecordGateway.findByTableAndField("dataphin_fashion_ods", "ods_hzct_user_info", "id_card_num"))
                .thenReturn(Optional.of(existingLockedRecord));

        int resultCount = appService.manualScan(RecognitionRuleManualScanDTO.builder().build());

        assertTrue(resultCount > 0);
        verify(sensitiveTaggingRecordGateway, atLeastOnce()).update(existingLockedRecord);

        assertEquals("旧自定义分类", existingLockedRecord.getCategoryName(), "人工锁定记录的当前分类不可被自动覆盖");
        assertTrue(existingLockedRecord.getHasBetterRecommendation(), "应标记存在更优推荐");
        assertEquals(1011L, existingLockedRecord.getRecommendedCategoryId(), "推荐分类应指向新命中的居民身份证");
    }
}
