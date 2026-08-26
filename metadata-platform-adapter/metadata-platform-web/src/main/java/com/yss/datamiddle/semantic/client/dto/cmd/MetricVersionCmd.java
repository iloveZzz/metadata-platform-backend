package com.yss.datamiddle.semantic.client.dto.cmd;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class MetricVersionCmd {
    @NotBlank(message = "计算逻辑表达式不能为空")
    private String expression;
    private String logicDescription;
    private List<String> dimensions;
    private String filters;
}
