package com.yss.datamiddle.semantic.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricCreateInput {
    private String name;
    private String metricGroup;
    private String description;
    private String owner;
}
