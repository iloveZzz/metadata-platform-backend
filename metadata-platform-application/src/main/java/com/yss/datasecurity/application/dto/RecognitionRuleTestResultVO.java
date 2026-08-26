package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleTestResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String projectOrDatasource;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String sampleValue;
    private String matchedCategory;
    private String matchedGrade;
    private Double confidence;
    private String matchedRule;
}
