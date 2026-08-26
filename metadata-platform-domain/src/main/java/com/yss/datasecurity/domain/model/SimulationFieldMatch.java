package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationFieldMatch {
    private String tableName;
    private String fieldName;
    private String sampleValue;
    private String matchedCategoryName;
    private String securityGradeName;
    private String matchedCondition;
}
