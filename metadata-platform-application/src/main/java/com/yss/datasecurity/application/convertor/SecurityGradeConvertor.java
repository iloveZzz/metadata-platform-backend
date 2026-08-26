package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface SecurityGradeConvertor {

    SecurityGradeVO toVO(SecurityGrade domain);
    List<SecurityGradeVO> toVOList(List<SecurityGrade> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "boundCategoriesCount", constant = "0")
    @Mapping(target = "referencedRulesCount", constant = "0")
    @Mapping(target = "activeFieldsCount", constant = "0")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SecurityGrade toDomain(SecurityGradeCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "boundCategoriesCount", ignore = true)
    @Mapping(target = "referencedRulesCount", ignore = true)
    @Mapping(target = "activeFieldsCount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateDomainFromDTO(SecurityGradeUpdateDTO dto, @MappingTarget SecurityGrade domain);
}
