package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.SensitiveRule;
import com.yss.datasecurity.repository.entity.SensitiveRulePO;
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
public interface SensitiveRulePOConvertor {

    @Mapping(target = "categoryScopeIds", source = "categoryScopeIds", qualifiedByName = "jsonToLongList")
    SensitiveRule toDomain(SensitiveRulePO po);

    List<SensitiveRule> toDomainList(List<SensitiveRulePO> poList);

    @Mapping(target = "categoryScopeIds", source = "categoryScopeIds", qualifiedByName = "longListToJson")
    SensitiveRulePO toPO(SensitiveRule domain);

    @Named("jsonToLongList")
    default List<Long> jsonToLongList(String str) {
        if (str == null || str.trim().isEmpty() || "[]".equals(str.trim())) {
            return new ArrayList<>();
        }
        String cleaned = str.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Named("longListToJson")
    default String longListToJson(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }
}
