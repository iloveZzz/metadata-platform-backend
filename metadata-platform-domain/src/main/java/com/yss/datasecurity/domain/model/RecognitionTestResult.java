package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionTestResult {
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
