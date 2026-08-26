package com.yss.datasecurity.application.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datasecurity.application.dto.RecognitionRuleCreateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestResultVO;
import com.yss.datasecurity.application.dto.RecognitionRuleUpdateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleVO;
import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.domain.model.RecognitionTestResult;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface RecognitionRuleConvertor {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "categoryScopeConfig", source = "categoryScopeConfig", qualifiedByName = "jsonToObject")
    @Mapping(target = "computeScopeConfig", source = "computeScopeConfig", qualifiedByName = "jsonToObject")
    @Mapping(target = "datasourceScopeConfig", source = "datasourceScopeConfig", qualifiedByName = "jsonToObject")
    RecognitionRuleVO toVO(RecognitionRule domain);

    List<RecognitionRuleVO> toVOList(List<RecognitionRule> domainList);

    RecognitionRuleTestResultVO toTestVO(RecognitionTestResult domain);
    List<RecognitionRuleTestResultVO> toTestVOList(List<RecognitionTestResult> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ENABLED")
    @Mapping(target = "taggedFieldsCount", constant = "0")
    @Mapping(target = "categoryScopeMode", defaultValue = "ALL")
    @Mapping(target = "scanSourceType", defaultValue = "COMPUTE_ENGINE")
    @Mapping(target = "priority", defaultValue = "50")
    @Mapping(target = "lineageInheritanceEnabled", defaultValue = "false")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "categoryScopeConfig", source = "categoryScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "computeScopeConfig", source = "computeScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "datasourceScopeConfig", source = "datasourceScopeConfig", qualifiedByName = "objectToJson")
    RecognitionRule toDomain(RecognitionRuleCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "taggedFieldsCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "categoryScopeConfig", source = "categoryScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "computeScopeConfig", source = "computeScopeConfig", qualifiedByName = "objectToJson")
    @Mapping(target = "datasourceScopeConfig", source = "datasourceScopeConfig", qualifiedByName = "objectToJson")
    void updateDomainFromDTO(RecognitionRuleUpdateDTO dto, @MappingTarget RecognitionRule domain);

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
