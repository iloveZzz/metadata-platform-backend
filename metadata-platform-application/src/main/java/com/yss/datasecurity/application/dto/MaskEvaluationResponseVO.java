package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskEvaluationResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean whitelisted;
    private Integer appliedRulesCount;
    private List<Map<String, Object>> maskedRows;
}
