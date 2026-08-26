package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import com.yss.metadata.repository.entity.LineageEdgePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 血缘边持久化转换器（MapStruct；Domain ↔ PO）。
 *
 * <p>枚举 ↔ 列小写字符串（auto-high/auto-mid/manual-high/low；sql/job/manual）。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface LineageEdgeConvertor {

    @Mapping(source = "fromAssetId", target = "fromAsset")
    @Mapping(source = "toAssetId", target = "toAsset")
    LineageEdgePO toPO(LineageEdge edge);

    @Mapping(source = "fromAsset", target = "fromAssetId")
    @Mapping(source = "toAsset", target = "toAssetId")
    LineageEdge toDomain(LineageEdgePO po);

    List<LineageEdge> toDomainList(List<LineageEdgePO> pos);

    default String mapType(LineageType type) {
        return type == null ? null : type.getValue();
    }

    default LineageType mapType(String value) {
        return LineageType.fromValue(value);
    }

    default String mapConfidence(LineageConfidence confidence) {
        return confidence == null ? null : confidence.getValue();
    }

    default LineageConfidence mapConfidence(String value) {
        return LineageConfidence.fromValue(value);
    }
}
