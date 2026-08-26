package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.SensitiveRecordConvertor;
import com.yss.datasecurity.application.dto.SensitiveRecordCalibrateDTO;
import com.yss.datasecurity.application.service.impl.SensitiveRecordAppServiceImpl;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveRecordAppServiceTest {

    @Mock
    private SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;

    @Mock
    private DataCategoryGateway dataCategoryGateway;

    @Mock
    private SecurityGradeGateway securityGradeGateway;

    private SensitiveRecordAppService sensitiveRecordAppService;

    @BeforeEach
    void setUp() {
        sensitiveRecordAppService = new SensitiveRecordAppServiceImpl(
            sensitiveTaggingRecordGateway,
            dataCategoryGateway,
            securityGradeGateway,
            org.mapstruct.factory.Mappers.<SensitiveRecordConvertor>getMapper(SensitiveRecordConvertor.class)
        );
    }

    @Test
    @DisplayName("测试人工校准覆盖打标并永久锁定 (MANUAL > RULE)")
    void testCalibrateRecord_Success() {
        Long recordId = 6001L;
        SensitiveTaggingRecord record = SensitiveTaggingRecord.builder()
            .id(recordId)
            .tableName("t_cust_info")
            .fieldName("mobile_phone")
            .categoryId(1L)
            .categoryName("旧分类")
            .securityGradeId(1001L)
            .securityGradeName("L1")
            .sourceType("RULE_AUTO")
            .isLocked(false)
            .build();

        DataCategory targetCategory = DataCategory.builder()
            .id(2L)
            .categoryName("个人手机号码")
            .build();

        SecurityGrade targetGrade = SecurityGrade.builder()
            .id(1003L)
            .gradeName("L3 敏感机密")
            .sensitivityScore(80)
            .build();

        when(sensitiveTaggingRecordGateway.findById(recordId)).thenReturn(Optional.of(record));
        when(dataCategoryGateway.findById(2L)).thenReturn(Optional.of(targetCategory));
        when(securityGradeGateway.findById(1003L)).thenReturn(Optional.of(targetGrade));

        SensitiveRecordCalibrateDTO dto = SensitiveRecordCalibrateDTO.builder()
            .recordId(recordId)
            .categoryId(2L)
            .securityGradeId(1003L)
            .lockPermanent(true)
            .build();

        sensitiveRecordAppService.calibrateRecord(dto);

        assertEquals(2L, record.getCategoryId());
        assertEquals("个人手机号码", record.getCategoryName());
        assertEquals(1003L, record.getSecurityGradeId());
        assertEquals("L3 敏感机密", record.getSecurityGradeName());
        assertEquals(80, record.getSensitivityScore());
        assertEquals("MANUAL_LOCKED", record.getSourceType());
        assertTrue(record.getIsLocked());

        verify(sensitiveTaggingRecordGateway).update(record);
    }
}
