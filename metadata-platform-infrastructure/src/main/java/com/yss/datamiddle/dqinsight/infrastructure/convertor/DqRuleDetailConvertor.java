package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.client.vo.RuleScoreVO;
import com.yss.datamiddle.dqinsight.domain.model.RuleScoreSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleDetailPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 规则明细快照转换（RuleScoreSnapshot → DqRuleDetailPO → RuleScoreVO，MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqRuleDetailConvertor {

    DqRuleDetailPO toPO(RuleScoreSnapshot snapshot);

    @Mapping(source = "executionTime", target = "toolTime")
    RuleScoreVO toRuleScoreVO(DqRuleDetailPO po);

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

    default String localDateTimeToString(LocalDateTime value) {
        return value == null ? null : IsoTimes.format(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
