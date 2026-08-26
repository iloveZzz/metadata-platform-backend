package com.yss.datamiddle.smartgovernance.domain.security.gateway;

import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;

import java.util.List;

public interface ClassificationRuleGateway {
    List<ClassificationRule> findByTemplateId(String templateId);

    void batchSave(List<ClassificationRule> rules);
}
