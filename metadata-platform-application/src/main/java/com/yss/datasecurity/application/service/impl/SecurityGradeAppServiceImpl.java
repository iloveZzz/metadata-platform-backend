package com.yss.datasecurity.application.service.impl;

import com.yss.datasecurity.application.convertor.SecurityGradeConvertor;
import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;
import com.yss.datasecurity.application.service.SecurityGradeAppService;
import com.yss.datasecurity.domain.exception.DataSecurityErrorCode;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.exception.SecurityGradeReferenceConflictException;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.model.SecurityGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityGradeAppServiceImpl implements SecurityGradeAppService {

    private final SecurityGradeGateway securityGradeGateway;
    private final SecurityGradeConvertor convertor;

    @Override
    public List<SecurityGradeVO> listAll() {
        List<SecurityGrade> list = securityGradeGateway.listAll();
        return convertor.toVOList(list);
    }

    @Override
    public SecurityGradeVO getDetail(Long id) {
        SecurityGrade grade = securityGradeGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.GRADE_NOT_FOUND, "数据分级不存在: " + id));
        return convertor.toVO(grade);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SecurityGradeCreateDTO dto) {
        securityGradeGateway.findByName(dto.getGradeName()).ifPresent(g -> {
            throw new DataSecurityException(DataSecurityErrorCode.GRADE_NAME_DUPLICATE, "分级名称已存在: " + dto.getGradeName());
        });
        securityGradeGateway.findByCode(dto.getGradeCode()).ifPresent(g -> {
            throw new DataSecurityException(DataSecurityErrorCode.GRADE_CODE_DUPLICATE, "分级缩写已存在: " + dto.getGradeCode());
        });

        SecurityGrade domain = convertor.toDomain(dto);
        domain.validateSensitivityScore();
        SecurityGrade saved = securityGradeGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SecurityGradeUpdateDTO dto) {
        SecurityGrade grade = securityGradeGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.GRADE_NOT_FOUND, "数据分级不存在: " + id));

        securityGradeGateway.findByName(dto.getGradeName()).ifPresent(g -> {
            if (!g.getId().equals(id)) {
                throw new DataSecurityException(DataSecurityErrorCode.GRADE_NAME_DUPLICATE, "分级名称已存在: " + dto.getGradeName());
            }
        });
        if (dto.getGradeCode() != null && !dto.getGradeCode().trim().isEmpty()) {
            securityGradeGateway.findByCode(dto.getGradeCode()).ifPresent(g -> {
                if (!g.getId().equals(id)) {
                    throw new DataSecurityException(DataSecurityErrorCode.GRADE_CODE_DUPLICATE, "分级缩写已存在: " + dto.getGradeCode());
                }
            });
        }

        convertor.updateDomainFromDTO(dto, grade);
        if (grade.getSensitivityScore() != null) {
            grade.validateSensitivityScore();
        }
        securityGradeGateway.update(grade);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SecurityGrade grade = securityGradeGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.GRADE_NOT_FOUND, "数据分级不存在: " + id));

        int boundCategories = securityGradeGateway.countBoundCategories(id);
        int referencedRules = securityGradeGateway.countReferencedRules(id);
        int totalRefs = boundCategories + referencedRules;

        if (totalRefs > 0) {
            throw new SecurityGradeReferenceConflictException(id, grade.getGradeName(), totalRefs);
        }

        securityGradeGateway.deleteById(id);
    }
}
