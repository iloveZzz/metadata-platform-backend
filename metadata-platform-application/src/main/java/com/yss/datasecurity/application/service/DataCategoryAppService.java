package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.CategoryActiveFieldVO;
import com.yss.datasecurity.application.dto.CategoryBatchDeleteDTO;
import com.yss.datasecurity.application.dto.CategoryBatchGradeDTO;
import com.yss.datasecurity.application.dto.CategoryBatchMoveDTO;
import com.yss.datasecurity.application.dto.CategoryBatchStatusDTO;
import com.yss.datasecurity.application.dto.CategoryStatusChangeDTO;
import com.yss.datasecurity.application.dto.DataCategoryCreateDTO;
import com.yss.datasecurity.application.dto.DataCategoryUpdateDTO;
import com.yss.datasecurity.application.dto.DataCategoryVO;

import java.util.List;

public interface DataCategoryAppService {
    PageResult<DataCategoryVO> pageCategories(int pageIndex, int pageSize, Long treeNodeId, String keyword, String status);
    DataCategoryVO getDetail(Long id);
    Long create(DataCategoryCreateDTO dto);
    void update(Long id, DataCategoryUpdateDTO dto);
    void delete(Long id);
    void changeStatus(Long id, CategoryStatusChangeDTO dto);
    void batchMove(CategoryBatchMoveDTO dto);
    void batchUpdateGrade(CategoryBatchGradeDTO dto);
    void batchChangeStatus(CategoryBatchStatusDTO dto);
    void batchDelete(CategoryBatchDeleteDTO dto);
    List<CategoryActiveFieldVO> getActiveFields(Long categoryId);
    List<DataCategoryVO> exportCategories(Long treeNodeId, String keyword, String status);
}
