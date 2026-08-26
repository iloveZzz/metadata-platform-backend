package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.repository.entity.ClassRulePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 分类规则持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ClassRuleConvertor {

    ClassRulePO toPO(ClassRule rule);

    ClassRule toDomain(ClassRulePO po);

    List<ClassRule> toDomainList(List<ClassRulePO> pos);

    default String mapType(ClassRuleType type) {
        return type == null ? null : type.getValue();
    }

    default ClassRuleType mapType(String value) {
        return ClassRuleType.fromValue(value);
    }
}
