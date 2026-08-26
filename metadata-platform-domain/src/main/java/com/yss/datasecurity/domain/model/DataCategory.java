package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCategory {
    private Long id;
    private String categoryName;
    private String categoryCode;
    private Long treeNodeId;
    private String treeNodeName;
    private Long securityGradeId;
    private String securityGradeName;
    private Integer sensitivityScore;
    private Integer priority; // 1~5, 1最高
    private List<String> recognitionFeatures;
    private String scanDimensionConfig; // JSON 存储 6 维高级特征
    private String status; // ENABLED / DISABLED
    private String disablePolicy; // RETAIN_TAGS / DELETE_TAGS
    private String description;
    private Integer activeFieldsCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
