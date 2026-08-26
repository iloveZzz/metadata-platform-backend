package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.FieldHealthVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 健康分转换（HealthScore ↔ DqHealthScorePO → 查询投影 VO，MapStruct）。
 *
 * <p>过期展示态由查询投影派生（validUntil &lt; now → state=expired / expired=true / band=null，C23）；
 * 无结果独立展示态不归入档位。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqHealthScoreConvertor {

    @Mapping(source = "band", target = "healthBand")
    DqHealthScorePO toPO(HealthScore score);

    @Mapping(source = "healthBand", target = "band")
    AssetHealthRowVO toAssetHealthRowVO(DqHealthScorePO po);

    @Mapping(source = "healthBand", target = "band")
    FieldHealthVO toFieldHealthVO(DqHealthScorePO po);

    @Mapping(source = "healthBand", target = "band")
    AssetHealthDetailVO toAssetHealthDetailVO(DqHealthScorePO po);

    @Mapping(source = "healthBand", target = "band")
    RuleDetailVO toRuleDetailVO(DqHealthScorePO po);

    /**
     * 资产级 + 字段级健康分详情（派生过期态）。
     */
    default AssetHealthDetailVO toAssetHealthDetailVO(DqHealthScorePO po, Instant now) {
        AssetHealthDetailVO vo = toAssetHealthDetailVO(po);
        if (isExpired(po, now)) {
            vo.setState(HealthState.EXPIRED);
            vo.setBand(null);
            vo.setExpired(true);
        }
        return vo;
    }

    /**
     * 规则明细钻取（派生过期态；batchNo / algorithm / rules 由 Gateway 补充）。
     */
    default RuleDetailVO toRuleDetailVO(DqHealthScorePO po, Instant now) {
        RuleDetailVO vo = toRuleDetailVO(po);
        if (isExpired(po, now)) {
            vo.setState(HealthState.EXPIRED);
            vo.setBand(null);
            vo.setExpired(true);
        }
        return vo;
    }

    /**
     * 资产级健康分行（派生过期态：expired=true + state=expired + band=null，与「无结果」不混淆）。
     */
    default AssetHealthRowVO toAssetHealthRowVO(DqHealthScorePO po, Instant now) {
        AssetHealthRowVO vo = toAssetHealthRowVO(po);
        vo.setHasResult(true);
        if (isExpired(po, now)) {
            vo.setState(HealthState.EXPIRED);
            vo.setBand(null);
            vo.setExpired(true);
        }
        return vo;
    }

    /**
     * 字段级健康分（低分字段 score &lt; 75 = 差档标记，OQ-01）。
     */
    default FieldHealthVO toFieldHealthVO(DqHealthScorePO po, Instant now) {
        FieldHealthVO vo = toFieldHealthVO(po);
        if (isExpired(po, now)) {
            vo.setState(HealthState.EXPIRED);
            vo.setBand(null);
            vo.setExpired(true);
        }
        vo.setLowScore(po.getScore() != null && po.getScore() < 75);
        return vo;
    }

    default boolean isExpired(DqHealthScorePO po, Instant now) {
        return po.getValidUntil() != null && now != null && now.isAfter(toInstant(po.getValidUntil()));
    }

    default String bandToString(HealthBand value) {
        return value == null ? null : value.getCode();
    }

    default HealthBand stringToBand(String value) {
        return HealthBand.fromCodeOrNull(value);
    }

    default String stateToString(HealthState value) {
        return value == null ? null : value.getCode();
    }

    default HealthState stringToState(String value) {
        return HealthState.fromCodeOrNull(value);
    }

    default LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    default String localDateTimeToString(LocalDateTime value) {
        return value == null ? null : IsoTimes.format(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
