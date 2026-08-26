package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.DefaultMaskingPolicyDTO;
import com.yss.datasecurity.application.dto.DefaultMaskingPolicyVO;
import com.yss.datasecurity.application.dto.MaskEvaluationResponseVO;
import com.yss.datasecurity.application.dto.MaskQueryEvaluationRequestDTO;
import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleTransferOwnerDTO;
import com.yss.datasecurity.application.dto.MaskingRuleUpdateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleVO;

public interface MaskingRuleAppService {
    PageResult<MaskingRuleVO> pageRules(int pageIndex, int pageSize, String keyword, String ruleType, Long categoryId, String applyScene);
    Long createRule(MaskingRuleCreateDTO dto);
    void updateRule(Long id, MaskingRuleUpdateDTO dto);
    void deleteRule(Long id);
    void updateStatus(Long id, String status);
    void transferOwner(MaskingRuleTransferOwnerDTO dto);
    DefaultMaskingPolicyVO getDefaultPolicy();
    void saveDefaultPolicy(DefaultMaskingPolicyDTO dto);
    MaskEvaluationResponseVO evaluateMaskQuery(MaskQueryEvaluationRequestDTO dto);
}
