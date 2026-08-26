package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.infrastructure.convertor.DataCategoryPOConvertor;
import com.yss.datasecurity.repository.entity.CategoryTreePO;
import com.yss.datasecurity.repository.entity.DataCategoryPO;
import com.yss.datasecurity.repository.mapper.CategoryTreeRepository;
import com.yss.datasecurity.repository.mapper.DataCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DataCategoryGatewayImpl implements DataCategoryGateway {

    private final DataCategoryRepository dataCategoryRepository;
    private final CategoryTreeRepository categoryTreeRepository;
    private final DataCategoryPOConvertor convertor = Mappers.getMapper(DataCategoryPOConvertor.class);

    @Override
    public List<DataCategory> pageCategories(int pageIndex, int pageSize, Long treeNodeId, String keyword, String status) {
        Page<DataCategoryPO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<DataCategoryPO> query = buildQuery(treeNodeId, keyword, status);
        query.orderByAsc(DataCategoryPO::getPriority).orderByDesc(DataCategoryPO::getCreatedAt);

        Page<DataCategoryPO> result = dataCategoryRepository.selectPage(page, query);
        return convertor.toDomainList(result.getRecords());
    }

    @Override
    public long countCategories(Long treeNodeId, String keyword, String status) {
        LambdaQueryWrapper<DataCategoryPO> query = buildQuery(treeNodeId, keyword, status);
        Long count = dataCategoryRepository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public List<DataCategory> listAll(Long treeNodeId, String keyword, String status) {
        LambdaQueryWrapper<DataCategoryPO> query = buildQuery(treeNodeId, keyword, status);
        query.orderByAsc(DataCategoryPO::getPriority).orderByDesc(DataCategoryPO::getCreatedAt);
        List<DataCategoryPO> list = dataCategoryRepository.selectList(query);
        return convertor.toDomainList(list);
    }

    @Override
    public List<DataCategory> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataCategoryPO> list = dataCategoryRepository.selectBatchIds(ids);
        return convertor.toDomainList(list);
    }

    @Override
    public Optional<DataCategory> findById(Long id) {
        DataCategoryPO po = dataCategoryRepository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public DataCategory save(DataCategory dataCategory) {
        DataCategoryPO po = convertor.toPO(dataCategory);
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
        dataCategoryRepository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public void update(DataCategory dataCategory) {
        DataCategoryPO po = convertor.toPO(dataCategory);
        po.setUpdatedAt(LocalDateTime.now());
        dataCategoryRepository.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        dataCategoryRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status, String disablePolicy) {
        DataCategoryPO po = dataCategoryRepository.selectById(id);
        if (po != null) {
            po.setStatus(status);
            if (disablePolicy != null) {
                po.setDisablePolicy(disablePolicy);
            }
            po.setUpdatedAt(LocalDateTime.now());
            dataCategoryRepository.updateById(po);
        }
    }

    @Override
    public void batchMove(List<Long> categoryIds, Long targetTreeNodeId) {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        dataCategoryRepository.update(null,
            new LambdaUpdateWrapper<DataCategoryPO>()
                .in(DataCategoryPO::getId, categoryIds)
                .set(DataCategoryPO::getTreeNodeId, targetTreeNodeId)
                .set(DataCategoryPO::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void batchUpdateGrade(List<Long> categoryIds, Long securityGradeId) {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        dataCategoryRepository.update(null,
            new LambdaUpdateWrapper<DataCategoryPO>()
                .in(DataCategoryPO::getId, categoryIds)
                .set(DataCategoryPO::getSecurityGradeId, securityGradeId)
                .set(DataCategoryPO::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void batchUpdateStatus(List<Long> categoryIds, String status, String disablePolicy) {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        LambdaUpdateWrapper<DataCategoryPO> wrapper = new LambdaUpdateWrapper<DataCategoryPO>()
            .in(DataCategoryPO::getId, categoryIds)
            .set(DataCategoryPO::getStatus, status)
            .set(DataCategoryPO::getUpdatedAt, LocalDateTime.now());
        if (disablePolicy != null) {
            wrapper.set(DataCategoryPO::getDisablePolicy, disablePolicy);
        }
        dataCategoryRepository.update(null, wrapper);
    }

    @Override
    public void batchDelete(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        dataCategoryRepository.deleteBatchIds(categoryIds);
    }

    @Override
    public Map<Long, Integer> countCategoriesGroupByTreeNode() {
        List<DataCategoryPO> all = dataCategoryRepository.selectList(
            new LambdaQueryWrapper<DataCategoryPO>()
                .select(DataCategoryPO::getTreeNodeId)
        );
        Map<Long, Integer> countMap = new HashMap<>();
        for (DataCategoryPO po : all) {
            if (po.getTreeNodeId() != null) {
                countMap.merge(po.getTreeNodeId(), 1, Integer::sum);
            }
        }
        return countMap;
    }

    private LambdaQueryWrapper<DataCategoryPO> buildQuery(Long treeNodeId, String keyword, String status) {
        LambdaQueryWrapper<DataCategoryPO> query = new LambdaQueryWrapper<>();
        if (treeNodeId != null && treeNodeId > 0) {
            CategoryTreePO node = categoryTreeRepository.selectById(treeNodeId);
            if (node != null) {
                String pathPrefix = node.getNodePath() != null ? node.getNodePath() : ("/" + node.getId());
                List<CategoryTreePO> descendantNodes = categoryTreeRepository.selectList(
                    new LambdaQueryWrapper<CategoryTreePO>()
                        .likeRight(CategoryTreePO::getNodePath, pathPrefix)
                        .or().eq(CategoryTreePO::getId, treeNodeId)
                        .or().eq(CategoryTreePO::getParentId, treeNodeId)
                );
                List<Long> nodeIds = descendantNodes.stream().map(CategoryTreePO::getId).collect(Collectors.toList());
                if (!nodeIds.isEmpty()) {
                    query.in(DataCategoryPO::getTreeNodeId, nodeIds);
                } else {
                    query.eq(DataCategoryPO::getTreeNodeId, treeNodeId);
                }
            } else {
                query.eq(DataCategoryPO::getTreeNodeId, treeNodeId);
            }
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(q -> q.like(DataCategoryPO::getCategoryName, keyword)
                         .or().like(DataCategoryPO::getCategoryCode, keyword));
        }
        if (status != null && !status.trim().isEmpty()) {
            query.eq(DataCategoryPO::getStatus, status);
        }
        return query;
    }
}
