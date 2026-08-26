package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.RuleSimulationRequestDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleUpdateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleVO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;

import java.util.List;

public interface SensitiveRuleAppService {
    PageResult<SensitiveRuleVO> pageRules(int pageIndex, int pageSize, String keyword, String status, String scanScopeType, String ruleType);
    SensitiveRuleVO getDetail(Long id);
    Long create(SensitiveRuleCreateDTO dto);
    void update(Long id, SensitiveRuleUpdateDTO dto);
    void delete(Long id);
    void updateStatus(Long id, String status);
    Long cloneRule(Long id);
    void resetRule(Long id);
    List<SimulationFieldMatchVO> simulate(RuleSimulationRequestDTO dto);
}
