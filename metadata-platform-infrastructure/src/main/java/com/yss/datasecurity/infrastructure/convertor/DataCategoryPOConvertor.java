package com.yss.datasecurity.infrastructure.convertor;

import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.repository.entity.DataCategoryPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(config = MapStructInfraConfig.class)
public interface DataCategoryPOConvertor {

    @Mapping(target = "treeNodeName", ignore = true)
    @Mapping(target = "securityGradeName", ignore = true)
    @Mapping(target = "sensitivityScore", ignore = true)
    @Mapping(target = "activeFieldsCount", constant = "0")
    @Mapping(target = "recognitionFeatures", source = "scanDimensionConfig", qualifiedByName = "jsonToStringList")
    DataCategory toDomain(DataCategoryPO po);

    List<DataCategory> toDomainList(List<DataCategoryPO> poList);

    @Mapping(target = "scanDimensionConfig", source = "domain", qualifiedByName = "domainToScanConfig")
    DataCategoryPO toPO(DataCategory domain);

    @Named("domainToScanConfig")
    default String domainToScanConfig(DataCategory domain) {
        if (domain == null) {
            return null;
        }
        String config = domain.getScanDimensionConfig() != null ? domain.getScanDimensionConfig().trim() : "";
        List<String> features = domain.getRecognitionFeatures() != null ? domain.getRecognitionFeatures() : new ArrayList<>();

        if (!config.isEmpty() && config.startsWith("{")) {
            if (!features.isEmpty() && !config.contains("\"recognitionFeatures\"")) {
                String featuresJson = stringListToJson(features);
                return "{\"recognitionFeatures\":" + featuresJson + "," + config.substring(1);
            }
            return config;
        }
        if (!features.isEmpty()) {
            return stringListToJson(features);
        }
        return config.isEmpty() ? null : config;
    }

    @Named("jsonToStringList")
    default List<String> jsonToStringList(String str) {
        if (str == null || str.trim().isEmpty() || "[]".equals(str.trim()) || "{}".equals(str.trim())) {
            return new ArrayList<>();
        }
        try {
            String trimmed = str.trim();
            if (trimmed.startsWith("{")) {
                if (trimmed.contains("\"recognitionFeatures\"") || trimmed.contains("'recognitionFeatures'")) {
                    int idx = trimmed.indexOf("recognitionFeatures");
                    int start = trimmed.indexOf("[", idx);
                    int end = trimmed.indexOf("]", start);
                    if (start >= 0 && end > start) {
                        trimmed = trimmed.substring(start, end + 1);
                    } else {
                        return new ArrayList<>();
                    }
                } else {
                    return new ArrayList<>();
                }
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
                if (trimmed.isEmpty()) {
                    return new ArrayList<>();
                }
                return Arrays.stream(trimmed.split(","))
                        .map(String::trim)
                        .map(s -> s.replace("\"", "").replace("'", "").trim())
                        .filter(s -> !s.isEmpty() && !s.startsWith("{") && !s.endsWith("}"))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Named("stringListToJson")
    default String stringListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return "[" + list.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(",")) + "]";
    }
}
