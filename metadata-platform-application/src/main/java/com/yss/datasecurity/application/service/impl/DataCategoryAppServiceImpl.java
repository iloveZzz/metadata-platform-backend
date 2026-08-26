package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.DataCategoryConvertor;
import com.yss.datasecurity.application.dto.CategoryActiveFieldVO;
import com.yss.datasecurity.application.dto.CategoryBatchDeleteDTO;
import com.yss.datasecurity.application.dto.CategoryBatchGradeDTO;
import com.yss.datasecurity.application.dto.CategoryBatchMoveDTO;
import com.yss.datasecurity.application.dto.CategoryBatchStatusDTO;
import com.yss.datasecurity.application.dto.CategoryStatusChangeDTO;
import com.yss.datasecurity.application.dto.DataCategoryCreateDTO;
import com.yss.datasecurity.application.dto.DataCategoryUpdateDTO;
import com.yss.datasecurity.application.dto.DataCategoryVO;
import com.yss.datasecurity.application.service.DataCategoryAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.CategoryTreeGateway;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.CategoryTreeNode;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataCategoryAppServiceImpl implements DataCategoryAppService {

    private final DataCategoryGateway dataCategoryGateway;
    private final SecurityGradeGateway securityGradeGateway;
    private final CategoryTreeGateway categoryTreeGateway;
    private final SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;
    private final DataCategoryConvertor convertor;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<DataCategoryVO> pageCategories(int pageIndex, int pageSize, Long treeNodeId, String keyword, String status) {
        List<DataCategory> list = dataCategoryGateway.pageCategories(pageIndex, pageSize, treeNodeId, keyword, status);
        long total = dataCategoryGateway.countCategories(treeNodeId, keyword, status);

        List<DataCategoryVO> voList = convertor.toVOList(list);
        for (DataCategoryVO vo : voList) {
            enrichVO(vo);
        }

        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    public DataCategoryVO getDetail(Long id) {
        DataCategory category = dataCategoryGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + id));
        DataCategoryVO vo = convertor.toVO(category);
        enrichVO(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DataCategoryCreateDTO dto) {
        SecurityGrade grade = securityGradeGateway.findById(dto.getSecurityGradeId())
            .orElseThrow(() -> new DataSecurityException("GRADE_NOT_FOUND", "关联数据分级不存在: " + dto.getSecurityGradeId()));
        CategoryTreeNode treeNode = categoryTreeGateway.findById(dto.getTreeNodeId())
            .orElseThrow(() -> new DataSecurityException("NODE_NOT_FOUND", "关联目录节点不存在: " + dto.getTreeNodeId()));

        DataCategory domain = convertor.toDomain(dto);
        if (domain.getCategoryCode() == null || domain.getCategoryCode().trim().isEmpty()) {
            domain.setCategoryCode("CAT_" + Math.abs(dto.getCategoryName().hashCode() % 100000));
        }
        domain.setSecurityGradeName(grade.getGradeName());
        domain.setSensitivityScore(grade.getSensitivityScore());
        domain.setTreeNodeName(treeNode.getNodeName());

        DataCategory saved = dataCategoryGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DataCategoryUpdateDTO dto) {
        DataCategory category = dataCategoryGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + id));

        SecurityGrade grade = securityGradeGateway.findById(dto.getSecurityGradeId())
            .orElseThrow(() -> new DataSecurityException("GRADE_NOT_FOUND", "关联数据分级不存在: " + dto.getSecurityGradeId()));
        CategoryTreeNode treeNode = categoryTreeGateway.findById(dto.getTreeNodeId())
            .orElseThrow(() -> new DataSecurityException("NODE_NOT_FOUND", "关联目录节点不存在: " + dto.getTreeNodeId()));

        convertor.updateDomainFromDTO(dto, category);
        if (category.getCategoryCode() == null || category.getCategoryCode().trim().isEmpty()) {
            category.setCategoryCode("CAT_" + Math.abs(dto.getCategoryName().hashCode() % 100000));
        }
        category.setSecurityGradeName(grade.getGradeName());
        category.setSensitivityScore(grade.getSensitivityScore());
        category.setTreeNodeName(treeNode.getNodeName());

        dataCategoryGateway.update(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        dataCategoryGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + id));
        dataCategoryGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, CategoryStatusChangeDTO dto) {
        dataCategoryGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + id));
        dataCategoryGateway.updateStatus(id, dto.getStatus(), dto.getDisablePolicy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMove(CategoryBatchMoveDTO dto) {
        CategoryTreeNode target = categoryTreeGateway.findById(dto.getTargetTreeNodeId())
            .orElseThrow(() -> new DataSecurityException("NODE_NOT_FOUND", "目标目录节点不存在: " + dto.getTargetTreeNodeId()));
        dataCategoryGateway.batchMove(dto.getCategoryIds(), target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateGrade(CategoryBatchGradeDTO dto) {
        SecurityGrade grade = securityGradeGateway.findById(dto.getSecurityGradeId())
            .orElseThrow(() -> new DataSecurityException("GRADE_NOT_FOUND", "目标数据分级不存在: " + dto.getSecurityGradeId()));
        dataCategoryGateway.batchUpdateGrade(dto.getCategoryIds(), grade.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchChangeStatus(CategoryBatchStatusDTO dto) {
        dataCategoryGateway.batchUpdateStatus(dto.getCategoryIds(), dto.getStatus(), dto.getDisablePolicy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(CategoryBatchDeleteDTO dto) {
        dataCategoryGateway.batchDelete(dto.getCategoryIds());
    }

    @Override
    public List<CategoryActiveFieldVO> getActiveFields(Long categoryId) {
        DataCategory category = dataCategoryGateway.findById(categoryId)
            .orElseThrow(() -> new DataSecurityException("CATEGORY_NOT_FOUND", "数据分类不存在: " + categoryId));

        List<SensitiveTaggingRecord> records = sensitiveTaggingRecordGateway.listByCategoryId(categoryId);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        return records.stream().map(r -> CategoryActiveFieldVO.builder()
            .id(r.getId())
            .categoryId(categoryId)
            .categoryName(category.getCategoryName())
            .fieldName(r.getFieldName())
            .fieldComment(r.getFieldComment())
            .tableName(r.getTableName())
            .dataSourceName(r.getDatasourceName() != null ? r.getDatasourceName() : r.getDatasourceId())
            .matchRule(r.getMatchedRuleName() != null ? r.getMatchedRuleName() : "-")
            .confidence(r.getConfidenceScore() != null ? String.format("%.1f%%", r.getConfidenceScore() * 100) : "100.0%")
            .lastScanTime(r.getUpdatedAt() != null ? r.getUpdatedAt().format(DATE_TIME_FORMATTER) :
                         r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_TIME_FORMATTER) : "-")
            .build()
        ).collect(Collectors.toList());
    }

    @Override
    public List<DataCategoryVO> exportCategories(Long treeNodeId, String keyword, String status) {
        List<DataCategory> list = dataCategoryGateway.listAll(treeNodeId, keyword, status);
        List<DataCategoryVO> voList = convertor.toVOList(list);
        for (DataCategoryVO vo : voList) {
            enrichVO(vo);
        }
        return voList;
    }

    private void enrichVO(DataCategoryVO vo) {
        if (vo.getSecurityGradeId() != null && vo.getSecurityGradeName() == null) {
            securityGradeGateway.findById(vo.getSecurityGradeId()).ifPresent(g -> {
                vo.setSecurityGradeName(g.getGradeName());
                vo.setSensitivityScore(g.getSensitivityScore());
            });
        }
        if (vo.getTreeNodeId() != null && vo.getTreeNodeName() == null) {
            categoryTreeGateway.findById(vo.getTreeNodeId()).ifPresent(t -> {
                vo.setTreeNodeName(t.getNodeName());
            });
        }
        if (vo.getId() != null) {
            long count = sensitiveTaggingRecordGateway.countRecords(null, vo.getId(), null, null, null);
            vo.setActiveFieldsCount((int) count);
        }
    }
}
