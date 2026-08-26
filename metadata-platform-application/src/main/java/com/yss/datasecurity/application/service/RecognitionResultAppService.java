package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RecognitionBatchLogVO;
import com.yss.datasecurity.application.dto.RecognitionResultDetailVO;
import com.yss.datasecurity.application.dto.RecognitionResultEditDTO;
import com.yss.datasecurity.application.dto.RecognitionResultImportPreviewVO;
import com.yss.datasecurity.application.dto.RecognitionResultManualAddDTO;
import com.yss.datasecurity.application.dto.RecognitionResultPageQueryDTO;
import com.yss.datasecurity.application.dto.RecognitionResultVO;

import java.util.List;

public interface RecognitionResultAppService {

    PageResult<RecognitionResultVO> pageRecognitionResults(RecognitionResultPageQueryDTO query);

    RecognitionResultDetailVO getRecognitionResultDetail(Long id);

    void updateMaskingStatus(Long id, String status);

    void batchUpdateMaskingStatus(List<Long> ids, String status);

    void lockResult(Long id, boolean isLocked);

    void batchLockResults(List<Long> ids, boolean isLocked);

    void editResult(RecognitionResultEditDTO dto);

    void batchEditResults(RecognitionResultEditDTO dto);

    void adoptRecommendation(Long id, Long candidateCategoryId);

    void deleteResult(Long id);

    void batchDeleteResults(List<Long> ids);

    void manualAdd(RecognitionResultManualAddDTO dto);

    RecognitionResultImportPreviewVO importPreview(String assetType, String conflictStrategy, String maskingPolicy);

    void importExecute(String assetType, String conflictStrategy, String maskingPolicy, String fileName);

    List<RecognitionBatchLogVO> listImportHistory();
}
