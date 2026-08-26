package com.yss.datamiddle.semantic.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricVersionInput {
    private String expression;
    private String logicDescription;
    private List<String> dimensions;
    private String filters;
}
