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
public class SimulationFieldMatchVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private String fieldName;
    private String sampleValue;
    private String matchedCategoryName;
    private String securityGradeName;
    private String matchedCondition;
}
