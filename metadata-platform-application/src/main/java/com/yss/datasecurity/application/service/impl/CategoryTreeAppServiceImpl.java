package com.yss.datasecurity.application.service.impl;

import com.yss.datasecurity.application.convertor.CategoryTreeConvertor;
import com.yss.datasecurity.application.dto.CategoryTreeNodeCreateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeUpdateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeVO;
import com.yss.datasecurity.application.service.CategoryTreeAppService;
import com.yss.datasecurity.domain.exception.CategoryDepthExceededException;
import com.yss.datasecurity.domain.exception.DataSecurityErrorCode;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.CategoryTreeGateway;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryTreeAppServiceImpl implements CategoryTreeAppService {

    private final CategoryTreeGateway categoryTreeGateway;
    private final DataCategoryGateway dataCategoryGateway;
    private final CategoryTreeConvertor convertor;

    @Override
    public List<CategoryTreeNodeVO> getTree() {
        List<CategoryTreeNode> allNodes = categoryTreeGateway.listAllNodes();
        Map<Long, Integer> countMap = dataCategoryGateway.countCategoriesGroupByTreeNode();
        return buildTree(allNodes, countMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNode(CategoryTreeNodeCreateDTO dto) {
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        int parentDepth = parentId == 0L ? 0 : categoryTreeGateway.getDepthLevel(parentId);

        if (parentDepth >= 10) {
            throw new CategoryDepthExceededException(parentDepth + 1);
        }

        CategoryTreeNode node = convertor.toDomain(dto);
        node.setParentId(parentId);
        node.setDepthLevel(parentDepth + 1);
        if (parentId == 0L) {
            node.setNodePath("/" + dto.getNodeName());
        } else {
            CategoryTreeNode parent = categoryTreeGateway.findById(parentId).orElse(null);
            String parentPath = (parent != null && parent.getNodePath() != null) ? parent.getNodePath() : ("/" + parentId);
            node.setNodePath(parentPath + "/" + dto.getNodeName());
        }

        CategoryTreeNode saved = categoryTreeGateway.save(node);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long id, CategoryTreeNodeUpdateDTO dto) {
        CategoryTreeNode node = categoryTreeGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.NODE_NOT_FOUND, "分类目录节点不存在: " + id));

        convertor.updateDomainFromDTO(dto, node);
        categoryTreeGateway.update(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) {
        CategoryTreeNode node = categoryTreeGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.NODE_NOT_FOUND, "分类目录节点不存在: " + id));

        categoryTreeGateway.deleteByIdCascade(id);
    }

    private List<CategoryTreeNodeVO> buildTree(List<CategoryTreeNode> nodes, Map<Long, Integer> countMap) {
        Map<Long, CategoryTreeNodeVO> map = new HashMap<>();
        List<CategoryTreeNodeVO> roots = new ArrayList<>();

        for (CategoryTreeNode node : nodes) {
            CategoryTreeNodeVO vo = convertor.toVO(node);
            vo.setCategoryCount(countMap != null ? countMap.getOrDefault(node.getId(), 0) : 0);
            vo.setChildren(new ArrayList<>());
            map.put(node.getId(), vo);
        }

        for (CategoryTreeNode node : nodes) {
            CategoryTreeNodeVO vo = map.get(node.getId());
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(vo);
            } else {
                CategoryTreeNodeVO parent = map.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }

        return roots;
    }
}
