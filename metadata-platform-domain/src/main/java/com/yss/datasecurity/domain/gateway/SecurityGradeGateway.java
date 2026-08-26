package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.SecurityGrade;

import java.util.List;
import java.util.Optional;

public interface SecurityGradeGateway {
    List<SecurityGrade> listAll();
    Optional<SecurityGrade> findById(Long id);
    Optional<SecurityGrade> findByName(String gradeName);
    Optional<SecurityGrade> findByCode(String gradeCode);
    SecurityGrade save(SecurityGrade securityGrade);
    void update(SecurityGrade securityGrade);
    void deleteById(Long id);
    int countBoundCategories(Long gradeId);
    int countReferencedRules(Long gradeId);
}
