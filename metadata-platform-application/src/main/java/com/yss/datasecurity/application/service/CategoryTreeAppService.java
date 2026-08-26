package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.dto.CategoryTreeNodeCreateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeUpdateDTO;
import com.yss.datasecurity.application.dto.CategoryTreeNodeVO;

import java.util.List;

public interface CategoryTreeAppService {
    List<CategoryTreeNodeVO> getTree();
    Long createNode(CategoryTreeNodeCreateDTO dto);
    void updateNode(Long id, CategoryTreeNodeUpdateDTO dto);
    void deleteNode(Long id);
}
