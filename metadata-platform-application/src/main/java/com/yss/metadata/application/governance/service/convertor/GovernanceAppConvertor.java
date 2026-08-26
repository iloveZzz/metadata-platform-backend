package com.yss.metadata.application.governance.service.convertor;

import com.yss.metadata.client.vo.ClassRuleVO;
import com.yss.metadata.client.vo.ClassificationOverviewVO;
import com.yss.metadata.client.vo.ClassificationVO;
import com.yss.metadata.client.vo.PropagateTaskVO;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.domain.governance.model.PropagateTaskStatus;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 治理域应用转换器（MapStruct；Domain → VO）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface GovernanceAppConvertor {

    ClassRuleVO toRuleVO(ClassRule rule);

    List<ClassRuleVO> toRuleVOList(List<ClassRule> rules);

    ClassificationVO toClassificationVO(Classification classification);

    List<ClassificationVO> toClassificationVOList(List<Classification> classifications);

    PropagateTaskVO toPropagateTaskVO(PropagateTask task);

    /**
     * 概览组合 VO（GET /api/classifications「识别结果 / 规则列表」）。
     */
    default ClassificationOverviewVO toOverviewVO(List<ClassRule> rules, List<Classification> classifications) {
        ClassificationOverviewVO vo = new ClassificationOverviewVO();
        vo.setRules(toRuleVOList(rules));
        vo.setResults(toClassificationVOList(classifications));
        return vo;
    }

    default String mapType(ClassRuleType type) {
        return type == null ? null : type.getValue();
    }

    default String mapStatus(ClassificationStatus status) {
        return status == null ? null : status.getValue();
    }

    default String mapTaskStatus(PropagateTaskStatus status) {
        return status == null ? null : status.getValue();
    }
}
