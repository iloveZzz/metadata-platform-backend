package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.repository.entity.DataCategoryPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructInfraConfig.class)
public interface DataCategoryPOConvertor {

    @Mapping(target = "treeNodeName", ignore = true)
    @Mapping(target = "securityGradeName", ignore = true)
    @Mapping(target = "sensitivityScore", ignore = true)
    @Mapping(target = "activeFieldsCount", constant = "0")
    DataCategory toDomain(DataCategoryPO po);

    List<DataCategory> toDomainList(List<DataCategoryPO> poList);

    DataCategoryPO toPO(DataCategory domain);
}
