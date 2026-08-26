package com.yss.metadata.application.governance.service;

import com.yss.metadata.client.dto.cmd.ClassRuleCmd;
import com.yss.metadata.client.vo.ClassRuleVO;
import com.yss.metadata.client.vo.ClassificationOverviewVO;
import com.yss.metadata.client.vo.ClassificationVO;
import com.yss.metadata.client.vo.PropagateTaskVO;

/**
 * 分级分类治理应用服务（FR-016 / FR-017）。
 *
 * <p>规则：创建/列表/启停（审计）；结果：候选确认/修正（幂等）；传播：沿血缘边
 * 同版本只跑一次幂等 + 覆盖范围可核验 + 审计。</p>
 */
public interface ClassificationGovernanceService {

    /**
     * 概览（GET /api/classifications：识别规则 + 识别结果一次返回；0 候选空结构非错误）。
     */
    ClassificationOverviewVO getOverview();

    /**
     * 新增/修正分类规则（POST /api/classifications configure；审计 classify.rule）。
     */
    ClassRuleVO createRule(ClassRuleCmd cmd, String operator);

    /**
     * 规则启停（PUT /api/classifications/{id}/status；幂等 + 审计 classify.rule.status）。
     */
    ClassRuleVO toggleRule(String id, boolean enabled, String operator);

    /**
     * 确认/修正候选分类（POST /api/classifications/{id}/confirm；确认幂等；
     * 修正以 correctedName 覆盖并流转已修正；冻结 spec 未声明 body，偏离登记）。
     */
    ClassificationVO confirm(String id, String correctedName);

    /**
     * 触发分类沿血缘传播（POST /api/classifications/{id}/propagate；202 语义；
     * 同 classification+version 只跑一次幂等；覆盖范围可核验；审计 classify.propagate）。
     */
    PropagateTaskVO propagate(String id, String operator);
}
