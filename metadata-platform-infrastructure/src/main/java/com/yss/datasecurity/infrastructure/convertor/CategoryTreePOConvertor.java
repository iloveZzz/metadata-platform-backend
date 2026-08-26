package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.CategoryTreeNode;
import com.yss.datasecurity.repository.entity.CategoryTreePO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(config = MapStructInfraConfig.class)
public interface CategoryTreePOConvertor {

    @Mapping(target = "admins", source = "admins", qualifiedByName = "jsonToList")
    @Mapping(target = "children", ignore = true)
    CategoryTreeNode toDomain(CategoryTreePO po);

    List<CategoryTreeNode> toDomainList(List<CategoryTreePO> poList);

    @Mapping(target = "admins", source = "admins", qualifiedByName = "listToJson")
    CategoryTreePO toPO(CategoryTreeNode domain);

    @Named("jsonToList")
    default List<String> jsonToList(String str) {
        if (str == null || str.trim().isEmpty() || "[]".equals(str.trim())) {
            return new ArrayList<>();
        }
        String cleaned = str.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Named("listToJson")
    default String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]";
    }
}
