package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.CategoryTreeNode;

import java.util.List;
import java.util.Optional;

public interface CategoryTreeGateway {
    List<CategoryTreeNode> listAllNodes();
    Optional<CategoryTreeNode> findById(Long id);
    CategoryTreeNode save(CategoryTreeNode node);
    void update(CategoryTreeNode node);
    void deleteByIdCascade(Long id);
    int getDepthLevel(Long parentId);
}
