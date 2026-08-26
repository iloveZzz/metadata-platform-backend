package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.DataCategoryConvertor;
import com.yss.datasecurity.application.dto.CategoryActiveFieldVO;
import com.yss.datasecurity.application.dto.CategoryBatchDeleteDTO;
import com.yss.datasecurity.application.dto.CategoryBatchGradeDTO;
import com.yss.datasecurity.application.dto.CategoryBatchMoveDTO;
import com.yss.datasecurity.application.dto.CategoryBatchStatusDTO;
import com.yss.datasecurity.application.dto.CategoryStatusChangeDTO;
import com.yss.datasecurity.application.dto.DataCategoryCreateDTO;
import com.yss.datasecurity.application.dto.DataCategoryVO;
import com.yss.datasecurity.application.service.impl.DataCategoryAppServiceImpl;
import com.yss.datasecurity.domain.gateway.CategoryTreeGateway;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataCategoryAppServiceTest {

    @Mock
    private DataCategoryGateway dataCategoryGateway;

    @Mock
    private SecurityGradeGateway securityGradeGateway;

    @Mock
    private CategoryTreeGateway categoryTreeGateway;

    @Mock
    private SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;

    private DataCategoryAppService dataCategoryAppService;

    @BeforeEach
    void setUp() {
        dataCategoryAppService = new DataCategoryAppServiceImpl(
            dataCategoryGateway,
            securityGradeGateway,
            categoryTreeGateway,
            sensitiveTaggingRecordGateway,
            org.mapstruct.factory.Mappers.getMapper(DataCategoryConvertor.class)
        );
    }

    @Test
    @DisplayName("测试创建数据分类 - 正确关联分级与目录并保存")
    void testCreateDataCategory_Success() {
        DataCategoryCreateDTO dto = DataCategoryCreateDTO.builder()
            .categoryName("手机号")
            .categoryCode("PHONE")
            .treeNodeId(10L)
            .securityGradeId(3L)
            .priority(2)
            .description("用户手机号")
            .build();

        when(securityGradeGateway.findById(3L)).thenReturn(Optional.of(SecurityGrade.builder().id(3L).gradeName("L3 敏感").sensitivityScore(60).build()));
        when(categoryTreeGateway.findById(10L)).thenReturn(Optional.of(CategoryTreeNode.builder().id(10L).nodeName("个人信息").build()));
        when(dataCategoryGateway.save(any(DataCategory.class))).thenAnswer(invocation -> {
            DataCategory c = invocation.getArgument(0);
            c.setId(3001L);
            return c;
        });

        Long id = dataCategoryAppService.create(dto);
        assertNotNull(id);
        assertEquals(3001L, id);
    }

    @Test
    @DisplayName("测试停用数据分类 - 支持保留打标策略")
    void testChangeStatus_RetainTagsPolicy() {
        Long catId = 3001L;
        DataCategory existing = DataCategory.builder().id(catId).categoryName("手机号").status("ENABLED").build();
        when(dataCategoryGateway.findById(catId)).thenReturn(Optional.of(existing));

        CategoryStatusChangeDTO dto = CategoryStatusChangeDTO.builder()
            .status("DISABLED")
            .disablePolicy("RETAIN_TAGS")
            .build();

        dataCategoryAppService.changeStatus(catId, dto);
        verify(dataCategoryGateway).updateStatus(catId, "DISABLED", "RETAIN_TAGS");
    }

    @Test
    @DisplayName("测试批量移动数据分类")
    void testBatchMove_Success() {
        CategoryBatchMoveDTO dto = CategoryBatchMoveDTO.builder()
            .categoryIds(Arrays.asList(101L, 102L))
            .targetTreeNodeId(200L)
            .build();

        when(categoryTreeGateway.findById(200L)).thenReturn(Optional.of(CategoryTreeNode.builder().id(200L).nodeName("新目录").build()));

        dataCategoryAppService.batchMove(dto);
        verify(dataCategoryGateway).batchMove(Arrays.asList(101L, 102L), 200L);
    }

    @Test
    @DisplayName("测试批量指定数据分级")
    void testBatchUpdateGrade_Success() {
        CategoryBatchGradeDTO dto = CategoryBatchGradeDTO.builder()
            .categoryIds(Arrays.asList(101L, 102L))
            .securityGradeId(4L)
            .build();

        when(securityGradeGateway.findById(4L)).thenReturn(Optional.of(SecurityGrade.builder().id(4L).gradeName("L4 绝密").build()));

        dataCategoryAppService.batchUpdateGrade(dto);
        verify(dataCategoryGateway).batchUpdateGrade(Arrays.asList(101L, 102L), 4L);
    }

    @Test
    @DisplayName("测试批量删除数据分类")
    void testBatchDelete_Success() {
        CategoryBatchDeleteDTO dto = CategoryBatchDeleteDTO.builder()
            .categoryIds(Arrays.asList(101L, 102L))
            .build();

        dataCategoryAppService.batchDelete(dto);
        verify(dataCategoryGateway).batchDelete(Arrays.asList(101L, 102L));
    }

    @Test
    @DisplayName("测试查询分类覆盖生效字段清单 - 真实调用 SensitiveTaggingRecordGateway")
    void testGetActiveFields_RealQuery() {
        Long catId = 101L;
        DataCategory existing = DataCategory.builder().id(catId).categoryName("车牌号").status("ENABLED").build();
        when(dataCategoryGateway.findById(catId)).thenReturn(Optional.of(existing));

        SensitiveTaggingRecord record = SensitiveTaggingRecord.builder()
            .id(501L)
            .categoryId(catId)
            .fieldName("plate_no")
            .fieldComment("车牌号")
            .tableName("dim_vehicle")
            .datasourceName("prod_db")
            .matchedRuleName("车牌正则")
            .confidenceScore(0.99)
            .createdAt(LocalDateTime.now())
            .build();

        when(sensitiveTaggingRecordGateway.listByCategoryId(catId)).thenReturn(Collections.singletonList(record));

        List<CategoryActiveFieldVO> fields = dataCategoryAppService.getActiveFields(catId);
        assertNotNull(fields);
        assertEquals(1, fields.size());
        assertEquals("plate_no", fields.get(0).getFieldName());
        assertEquals("车牌号", fields.get(0).getCategoryName());
        assertEquals("99.0%", fields.get(0).getConfidence());
    }

    @Test
    @DisplayName("测试全量导出数据分类列表")
    void testExportCategories_Success() {
        when(dataCategoryGateway.listAll(null, null, null)).thenReturn(Collections.singletonList(
            DataCategory.builder().id(101L).categoryName("车牌号").securityGradeId(2L).treeNodeId(10L).build()
        ));
        when(securityGradeGateway.findById(2L)).thenReturn(Optional.of(SecurityGrade.builder().id(2L).gradeName("L2 内部").sensitivityScore(40).build()));
        when(categoryTreeGateway.findById(10L)).thenReturn(Optional.of(CategoryTreeNode.builder().id(10L).nodeName("个人信息").build()));
        when(sensitiveTaggingRecordGateway.countRecords(null, 101L, null, null, null)).thenReturn(5L);

        List<DataCategoryVO> result = dataCategoryAppService.exportCategories(null, null, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("L2 内部", result.get(0).getSecurityGradeName());
        assertEquals("个人信息", result.get(0).getTreeNodeName());
        assertEquals(5, result.get(0).getActiveFieldsCount());
    }
}
