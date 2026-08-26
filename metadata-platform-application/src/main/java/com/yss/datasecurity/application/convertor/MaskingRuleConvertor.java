package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.MaskingRuleCreateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleUpdateDTO;
import com.yss.datasecurity.application.dto.MaskingRuleVO;
import com.yss.datasecurity.domain.model.MaskingRule;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface MaskingRuleConvertor {

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    MaskingRuleVO toVO(MaskingRule domain);
    List<MaskingRuleVO> toVOList(List<MaskingRule> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "keyName", ignore = true)
    @Mapping(target = "status", source = "status", defaultExpression = "java(\"ENABLED\")")
    @Mapping(target = "owner", source = "owner", defaultExpression = "java(\"安全管理员\")")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MaskingRule toDomain(MaskingRuleCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "keyName", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MaskingRule toDomain(MaskingRuleUpdateDTO dto);
}
