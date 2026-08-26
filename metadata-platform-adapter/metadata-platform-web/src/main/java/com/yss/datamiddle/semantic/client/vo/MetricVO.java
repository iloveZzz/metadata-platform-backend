package com.yss.datamiddle.semantic.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String metricGroup;
    private String description;
    private String owner;
    private String status;
    private Boolean authoritative;
    private Integer currentVersionNo;
    private LocalDateTime updatedAt;
}
