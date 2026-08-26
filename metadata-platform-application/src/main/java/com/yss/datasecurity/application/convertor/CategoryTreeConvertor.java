package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.CategoryTreeNodeCreateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeUpdateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeVO;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface CategoryTreeConvertor {

    CategoryTreeNodeVO toVO(CategoryTreeNode domain);
    List<CategoryTreeNodeVO> toVOList(List<CategoryTreeNode> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nodePath", ignore = true)
    @Mapping(target = "depthLevel", ignore = true)
    @Mapping(target = "sortOrder", constant = "0")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "children", ignore = true)
    CategoryTreeNode toDomain(CategoryTreeNodeCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "nodePath", ignore = true)
    @Mapping(target = "depthLevel", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateDomainFromDTO(CategoryTreeNodeUpdateDTO dto, @MappingTarget CategoryTreeNode domain);
}
