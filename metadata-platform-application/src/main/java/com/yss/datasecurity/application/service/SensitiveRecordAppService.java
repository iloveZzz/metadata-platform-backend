package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.SensitiveRecordCalibrateDTO;
import com.yss.datasecurity.application.dto.SensitiveRecordVO;

public interface SensitiveRecordAppService {
    PageResult<SensitiveRecordVO> pageRecords(int pageIndex, int pageSize, String keyword, Long categoryId, Long gradeId, Boolean isLocked, String datasourceId);
    void calibrateRecord(SensitiveRecordCalibrateDTO dto);
}
