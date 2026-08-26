package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCategoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String categoryName;
    private String categoryCode;
    private Long treeNodeId;
    private String treeNodeName;
    private Long securityGradeId;
    private String securityGradeName;
    private Integer sensitivityScore;
    private Integer priority;
    private String status;
    private String disablePolicy;
    private String description;
    private List<String> recognitionFeatures;
    private Integer activeFieldsCount;
}
