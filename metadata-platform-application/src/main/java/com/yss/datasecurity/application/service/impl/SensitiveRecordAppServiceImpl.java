package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.SensitiveRecordConvertor;
import com.yss.datasecurity.application.dto.SensitiveRecordCalibrateDTO;
import com.yss.datasecurity.application.dto.SensitiveRecordVO;
import com.yss.datasecurity.application.service.SensitiveRecordAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveRecordAppServiceImpl implements SensitiveRecordAppService {

    private final SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;
    private final DataCategoryGateway dataCategoryGateway;
    private final SecurityGradeGateway securityGradeGateway;
    private final SensitiveRecordConvertor convertor;

    @Override
    public PageResult<SensitiveRecordVO> pageRecords(int pageIndex, int pageSize, String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId) {
        List<SensitiveTaggingRecord> list = sensitiveTaggingRecordGateway.pageRecords(pageIndex, pageSize, keyword, categoryId, gradeId, isLocked, datasourceId);
        long total = sensitiveTaggingRecordGateway.countRecords(keyword, categoryId, gradeId, isLocked, datasourceId);
        List<SensitiveRecordVO> voList = convertor.toRecordVOList(list);
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calibrateRecord(SensitiveRecordCalibrateDTO dto) {
        SensitiveTaggingRecord record = sensitiveTaggingRecordGateway.findById(dto.getRecordId())
            .orElseThrow(() -> new DataSecurityException("RECORD_NOT_FOUND", "敏感识别打标记录不存在: " + dto.getRecordId()));

        DataCategory category = dataCategoryGateway.findById(dto.getCategoryId())
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "目标数据分类不存在: " + dto.getCategoryId()));

        SecurityGrade grade = securityGradeGateway.findById(dto.getSecurityGradeId())
            .orElseThrow(() -> new DataSecurityException("GRADE_NOT_FOUND", "目标安全分级不存在: " + dto.getSecurityGradeId()));

        record.calibrate(
            category.getId(),
            category.getCategoryName(),
            grade.getId(),
            grade.getGradeName(),
            grade.getSensitivityScore(),
            Boolean.TRUE.equals(dto.getLockPermanent()),
            "admin"
        );

        sensitiveTaggingRecordGateway.update(record);
    }
}
