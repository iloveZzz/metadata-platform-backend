package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.DataCategory;

import java.util.List;
import java.util.Optional;

public interface DataCategoryGateway {
    List<DataCategory> pageCategories(int pageIndex, int pageSize, Long treeNodeId, String keyword, String status);
    long countCategories(Long treeNodeId, String keyword, String status);
    List<DataCategory> listAll(Long treeNodeId, String keyword, String status);
    List<DataCategory> listByIds(List<Long> ids);
    Optional<DataCategory> findById(Long id);
    DataCategory save(DataCategory dataCategory);
    void update(DataCategory dataCategory);
    void deleteById(Long id);
    void updateStatus(Long id, String status, String disablePolicy);
    void batchMove(List<Long> categoryIds, Long targetTreeNodeId);
    void batchUpdateGrade(List<Long> categoryIds, Long securityGradeId);
    void batchUpdateStatus(List<Long> categoryIds, String status, String disablePolicy);
    void batchDelete(List<Long> categoryIds);
}
