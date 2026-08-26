package com.yss.datasecurity.domain.exception;

public class SecurityGradeReferenceConflictException extends DataSecurityException {
    public SecurityGradeReferenceConflictException(Long gradeId, String gradeName, int activeReferences) {
        super("GRADE_REFERENCE_CONFLICT", 
            String.format("数据分级 [%s] (ID: %d) 当前存在 %d 个关联引用（数据分类或识别规则），系统强制禁止物理删除！", 
                gradeName, gradeId, activeReferences));
    }
}
