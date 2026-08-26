package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleResultPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 规则结果转换（RuleResultRow → DqRuleResultPO，MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqRuleResultConvertor {

    DqRuleResultPO toPO(RuleResultRow row);

    RuleResultRow toDomain(DqRuleResultPO po);

    default String ruleTypeToString(RuleType value) {
        return value == null ? null : value.getCode();
    }

    default RuleType stringToRuleType(String value) {
        return RuleType.fromCodeOrNull(value);
    }

    default String ruleStatusToString(RuleStatus value) {
        return value == null ? null : value.getCode();
    }

    default RuleStatus stringToRuleStatus(String value) {
        return RuleStatus.fromCodeOrNull(value);
    }

    default LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant localDateTimeToInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
