package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.datasecurity.domain.gateway.CategoryTreeGateway;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import com.yss.datasecurity.infrastructure.convertor.CategoryTreePOConvertor;
import com.yss.datasecurity.repository.entity.CategoryTreePO;
import com.yss.datasecurity.repository.entity.DataCategoryPO;
import com.yss.datasecurity.repository.mapper.CategoryTreeRepository;
import com.yss.datasecurity.repository.mapper.DataCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryTreeGatewayImpl implements CategoryTreeGateway {

    private final CategoryTreeRepository categoryTreeRepository;
    private final DataCategoryRepository dataCategoryRepository;
    private final CategoryTreePOConvertor convertor = Mappers.getMapper(CategoryTreePOConvertor.class);

    @Override
    public List<CategoryTreeNode> listAllNodes() {
        LambdaQueryWrapper<CategoryTreePO> query = new LambdaQueryWrapper<CategoryTreePO>()
            .orderByAsc(CategoryTreePO::getDepthLevel)
            .orderByAsc(CategoryTreePO::getSortOrder);
        List<CategoryTreePO> pos = categoryTreeRepository.selectList(query);
        return convertor.toDomainList(pos);
    }

    @Override
    public Optional<CategoryTreeNode> findById(Long id) {
        CategoryTreePO po = categoryTreeRepository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public CategoryTreeNode save(CategoryTreeNode node) {
        CategoryTreePO po = convertor.toPO(node);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        if (po.getCreatedBy() == null) {
            po.setCreatedBy("system");
        }
        if (po.getUpdatedBy() == null) {
            po.setUpdatedBy("system");
        }
        categoryTreeRepository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(CategoryTreeNode node) {
        CategoryTreePO po = convertor.toPO(node);
        po.setUpdatedAt(LocalDateTime.now());
        categoryTreeRepository.updateById(po);
    }

    @Override
    public void deleteByIdCascade(Long id) {
        List<Long> allSubNodeIds = new ArrayList<>();
        collectChildNodeIds(id, allSubNodeIds);
        allSubNodeIds.add(id);

        for (Long nodeId : allSubNodeIds) {
            // 删除关联数据分类
            LambdaQueryWrapper<DataCategoryPO> catQuery = new LambdaQueryWrapper<DataCategoryPO>()
                .eq(DataCategoryPO::getTreeNodeId, nodeId);
            dataCategoryRepository.delete(catQuery);

            categoryTreeRepository.deleteById(nodeId);
        }
    }

    @Override
    public int getDepthLevel(Long parentId) {
        CategoryTreePO parent = categoryTreeRepository.selectById(parentId);
        return parent == null ? 0 : parent.getDepthLevel();
    }

    private void collectChildNodeIds(Long parentId, List<Long> result) {
        LambdaQueryWrapper<CategoryTreePO> query = new LambdaQueryWrapper<CategoryTreePO>()
            .eq(CategoryTreePO::getParentId, parentId);
        List<CategoryTreePO> children = categoryTreeRepository.selectList(query);
        for (CategoryTreePO child : children) {
            result.add(child.getId());
            collectChildNodeIds(child.getId(), result);
        }
    }
}
