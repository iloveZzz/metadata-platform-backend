package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RecognitionRuleBatchRunDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleCreateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleManualScanDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestResultVO;
import com.yss.datasecurity.application.dto.RecognitionRuleTransferOwnerDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleUpdateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleVO;

import java.util.List;

public interface RecognitionRuleAppService {
    PageResult<RecognitionRuleVO> pageRules(int pageIndex, int pageSize, String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername);
    RecognitionRuleVO getDetail(Long id);
    Long create(RecognitionRuleCreateDTO dto);
    void update(Long id, RecognitionRuleUpdateDTO dto);
    void delete(Long id);
    void updateStatus(Long id, String status);
    void resetRule(Long id);
    Long cloneRule(Long id);
    void transferOwner(RecognitionRuleTransferOwnerDTO dto);
    int batchRun(RecognitionRuleBatchRunDTO dto);
    int manualScan(RecognitionRuleManualScanDTO dto);
    List<RecognitionRuleTestResultVO> testRule(RecognitionRuleTestDTO dto);
}
