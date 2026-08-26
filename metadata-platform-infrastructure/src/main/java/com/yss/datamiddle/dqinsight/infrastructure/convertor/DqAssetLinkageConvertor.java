package com.yss.datamiddle.dqinsight.infrastructure.convertor;

import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.LinkageMatchMode;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.repository.entity.DqAssetLinkagePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 资产关联转换（AssetLinkage → DqAssetLinkagePO，MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface DqAssetLinkageConvertor {

    DqAssetLinkagePO toPO(AssetLinkage linkage);

    @ObjectFactory
    default AssetLinkage createAssetLinkage() {
        return AssetLinkage.forPersistenceLoad();
    }

    AssetLinkage toDomain(DqAssetLinkagePO po);

    default String linkageMatchModeToString(LinkageMatchMode value) {
        return value == null ? null : value.getCode();
    }

    default LinkageMatchMode stringToLinkageMatchMode(String value) {
        return LinkageMatchMode.fromCode(value);
    }

    default String linkageStateToString(LinkageState value) {
        return value == null ? null : value.getCode();
    }

    default LinkageState stringToLinkageState(String value) {
        return LinkageState.fromCodeOrNull(value);
    }

    default LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant localDateTimeToInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
