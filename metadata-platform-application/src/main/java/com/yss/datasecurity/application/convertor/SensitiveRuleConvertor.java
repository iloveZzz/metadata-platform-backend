package com.yss.datasecurity.application.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleUpdateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleVO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;
import com.yss.datasecurity.domain.model.SensitiveRule;
import com.yss.datasecurity.domain.model.SimulationFieldMatch;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface SensitiveRuleConvertor {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "featureConfig", source = "featureConfig", qualifiedByName = "jsonToObject")
    SensitiveRuleVO toVO(SensitiveRule domain);

    List<SensitiveRuleVO> toVOList(List<SensitiveRule> domainList);

    SimulationFieldMatchVO toSimulationVO(SimulationFieldMatch domain);
    List<SimulationFieldMatchVO> toSimulationVOList(List<SimulationFieldMatch> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", constant = "system")
    @Mapping(target = "status", constant = "ENABLED")
    @Mapping(target = "taggedFieldsCount", constant = "0")
    @Mapping(target = "categoryScopeMode", defaultValue = "ALL")
    @Mapping(target = "priority", defaultValue = "50")
    @Mapping(target = "scanScopeType", defaultValue = "DATASOURCE")
    @Mapping(target = "ruleType", defaultValue = "CUSTOM")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scanScopeConfig", source = "scanScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "featureConfig", source = "featureConfig", qualifiedByName = "objectToJson")
    SensitiveRule toDomain(SensitiveRuleCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "taggedFieldsCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scanScopeConfig", source = "scanScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "featureConfig", source = "featureConfig", qualifiedByName = "objectToJson")
    void updateDomainFromDTO(SensitiveRuleUpdateDTO dto, @MappingTarget SensitiveRule domain);

    @Named("objectToJson")
    default String objectToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @Named("jsonToObject")
    default Object jsonToObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
