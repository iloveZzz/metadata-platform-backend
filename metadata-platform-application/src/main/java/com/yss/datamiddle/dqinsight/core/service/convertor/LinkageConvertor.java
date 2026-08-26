package com.yss.datamiddle.dqinsight.core.service.convertor;

import com.yss.datamiddle.dqinsight.client.vo.LinkageResultVO;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.util.IsoTimes;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

/**
 * 关联映射结果转换（AssetLinkage → LinkageResultVO，MapStruct，C12）。
 */
@Mapper(config = MapStructAppConfig.class)
public interface LinkageConvertor {

    @Mapping(source = "resolvedAssetId", target = "assetId")
    LinkageResultVO toVO(AssetLinkage linkage);

    default String longToString(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    default String instantToString(Instant value) {
        return value == null ? null : IsoTimes.format(value);
    }
}
