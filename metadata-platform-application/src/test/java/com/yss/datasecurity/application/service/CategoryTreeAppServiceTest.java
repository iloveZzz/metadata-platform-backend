package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.convertor.CategoryTreeConvertor;
import com.yss.datasecurity.application.dto.CategoryTreeNodeCreateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeVO;
import com.yss.datasecurity.application.service.impl.CategoryTreeAppServiceImpl;
import com.yss.datasecurity.domain.exception.CategoryDepthExceededException;
import com.yss.datasecurity.domain.gateway.CategoryTreeGateway;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryTreeAppServiceTest {

    @Mock
    private CategoryTreeGateway categoryTreeGateway;

    private CategoryTreeAppService categoryTreeAppService;

    @BeforeEach
    void setUp() {
        categoryTreeAppService = new CategoryTreeAppServiceImpl(
            categoryTreeGateway,
            org.mapstruct.factory.Mappers.getMapper(CategoryTreeConvertor.class)
        );
    }

    @Test
    @DisplayName("测试分类目录层级限制 - 超过 10 级时抛出 CategoryDepthExceededException")
    void testCreateNode_DepthExceeded() {
        CategoryTreeNodeCreateDTO dto = CategoryTreeNodeCreateDTO.builder()
            .parentId(999L)
            .nodeName("第11级深层分类")
            .build();

        when(categoryTreeGateway.getDepthLevel(999L)).thenReturn(10);

        assertThrows(CategoryDepthExceededException.class, () -> categoryTreeAppService.createNode(dto));
    }

    @Test
    @DisplayName("测试分类目录树构建 - 正确组装父子层级关系")
    void testGetTree_HierarchyAssembly() {
        CategoryTreeNode root = CategoryTreeNode.builder()
            .id(1L)
            .parentId(0L)
            .nodeName("金融数据")
            .depthLevel(1)
            .build();

        CategoryTreeNode child = CategoryTreeNode.builder()
            .id(2L)
            .parentId(1L)
            .nodeName("个人信息")
            .depthLevel(2)
            .build();

        when(categoryTreeGateway.listAllNodes()).thenReturn(Arrays.asList(root, child));

        List<CategoryTreeNodeVO> tree = categoryTreeAppService.getTree();
        assertEquals(1, tree.size());
        assertEquals("金融数据", tree.get(0).getNodeName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("个人信息", tree.get(0).getChildren().get(0).getNodeName());
    }
}
