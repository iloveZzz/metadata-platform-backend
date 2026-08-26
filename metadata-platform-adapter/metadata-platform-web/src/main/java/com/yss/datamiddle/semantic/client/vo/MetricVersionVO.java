package com.yss.datamiddle.semantic.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricVersionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer versionNo;
    private String expression;
    private String logicDescription;
    private List<String> dimensions;
    private String filters;
    private Integer rollbackFromNo;
    private String createdBy;
    private LocalDateTime createdAt;
}
