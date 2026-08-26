package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.SecurityGradeConvertor;
import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;
import com.yss.datasecurity.application.service.impl.SecurityGradeAppServiceImpl;
import com.yss.datasecurity.domain.exception.SecurityGradeReferenceConflictException;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.model.SecurityGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityGradeAppServiceTest {

    @Mock
    private SecurityGradeGateway securityGradeGateway;

    private SecurityGradeAppService securityGradeAppService;

    @BeforeEach
    void setUp() {
        securityGradeAppService = new SecurityGradeAppServiceImpl(
            securityGradeGateway,
            org.mapstruct.factory.Mappers.getMapper(SecurityGradeConvertor.class)
        );
    }

    @Test
    @DisplayName("测试创建数据分级 - 敏感程度在 1~100 范围内成功创建")
    void testCreateSecurityGrade_Success() {
        SecurityGradeCreateDTO dto = SecurityGradeCreateDTO.builder()
            .gradeName("L4_绝密高危")
            .gradeCode("L4")
            .sensitivityScore(85)
            .colorTag("red")
            .description("企业绝密高危核心数据")
            .build();

        when(securityGradeGateway.findByName("L4_绝密高危")).thenReturn(Optional.empty());
        when(securityGradeGateway.findByCode("L4")).thenReturn(Optional.empty());
        when(securityGradeGateway.save(any(SecurityGrade.class))).thenAnswer(invocation -> {
            SecurityGrade g = invocation.getArgument(0);
            g.setId(1004L);
            return g;
        });

        Long id = securityGradeAppService.create(dto);
        assertNotNull(id);
        assertEquals(1004L, id);
    }

    @Test
    @DisplayName("测试删除数据分级 - 当存在关联引用时强制拦截并抛出 409 冲突异常")
    void testDeleteSecurityGrade_ConflictInterception() {
        Long gradeId = 1004L;
        SecurityGrade existing = SecurityGrade.builder()
            .id(gradeId)
            .gradeName("L4_绝密高危")
            .gradeCode("L4")
            .sensitivityScore(85)
            .build();

        when(securityGradeGateway.findById(gradeId)).thenReturn(Optional.of(existing));
        when(securityGradeGateway.countBoundCategories(gradeId)).thenReturn(5); // 存在5个分类引用
        when(securityGradeGateway.countReferencedRules(gradeId)).thenReturn(2); // 存在2个规则引用

        SecurityGradeReferenceConflictException exception = assertThrows(
            SecurityGradeReferenceConflictException.class,
            () -> securityGradeAppService.delete(gradeId)
        );

        assertEquals("GRADE_REFERENCE_CONFLICT", exception.getCode());
        verify(securityGradeGateway, never()).deleteById(gradeId);
    }

    @Test
    @DisplayName("测试删除数据分级 - 无关联引用时允许物理删除")
    void testDeleteSecurityGrade_SuccessWhenNoReferences() {
        Long gradeId = 1001L;
        SecurityGrade existing = SecurityGrade.builder()
            .id(gradeId)
            .gradeName("L1_对外公开")
            .gradeCode("L1")
            .sensitivityScore(20)
            .build();

        when(securityGradeGateway.findById(gradeId)).thenReturn(Optional.of(existing));
        when(securityGradeGateway.countBoundCategories(gradeId)).thenReturn(0);
        when(securityGradeGateway.countReferencedRules(gradeId)).thenReturn(0);

        securityGradeAppService.delete(gradeId);
        verify(securityGradeGateway).deleteById(gradeId);
    }

    @Test
    @DisplayName("测试更新数据分级 - 成功修改分级名称与分级描述")
    void testUpdateSecurityGrade_Success() {
        Long gradeId = 1002L;
        SecurityGrade existing = SecurityGrade.builder()
            .id(gradeId)
            .gradeName("L2_内部数据")
            .gradeCode("L2")
            .sensitivityScore(40)
            .description("原有描述")
            .build();

        com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO updateDTO = com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO.builder()
            .gradeName("L2_内部公开数据")
            .description("更新后的分级描述说明")
            .build();

        when(securityGradeGateway.findById(gradeId)).thenReturn(Optional.of(existing));
        when(securityGradeGateway.findByName("L2_内部公开数据")).thenReturn(Optional.empty());

        securityGradeAppService.update(gradeId, updateDTO);

        verify(securityGradeGateway).update(any(SecurityGrade.class));
        assertEquals("L2_内部公开数据", existing.getGradeName());
        assertEquals("更新后的分级描述说明", existing.getDescription());
    }
}
