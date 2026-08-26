package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.DataCategoryCreateDTO;
import com.yss.datasecurity.application.dto.DataCategoryUpdateDTO;
import com.yss.datasecurity.application.dto.DataCategoryVO;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface DataCategoryConvertor {

    DataCategoryVO toVO(DataCategory domain);
    List<DataCategoryVO> toVOList(List<DataCategory> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "treeNodeName", ignore = true)
    @Mapping(target = "securityGradeName", ignore = true)
    @Mapping(target = "sensitivityScore", ignore = true)
    @Mapping(target = "status", constant = "ENABLED")
    @Mapping(target = "disablePolicy", constant = "RETAIN_TAGS")
    @Mapping(target = "activeFieldsCount", constant = "0")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scanDimensionConfig", source = "scanDimensionConfig", qualifiedByName = "objectToString")
    DataCategory toDomain(DataCategoryCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "treeNodeName", ignore = true)
    @Mapping(target = "securityGradeName", ignore = true)
    @Mapping(target = "sensitivityScore", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "disablePolicy", ignore = true)
    @Mapping(target = "activeFieldsCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scanDimensionConfig", source = "scanDimensionConfig", qualifiedByName = "objectToString")
    void updateDomainFromDTO(DataCategoryUpdateDTO dto, @MappingTarget DataCategory domain);

    @Named("objectToString")
    default String objectToString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}
