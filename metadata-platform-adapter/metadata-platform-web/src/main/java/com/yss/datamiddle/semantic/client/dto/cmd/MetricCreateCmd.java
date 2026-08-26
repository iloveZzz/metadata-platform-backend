package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class MetricCreateCmd extends CommandDTO {
    @NotBlank(message = "指标名称不能为空")
    @Size(max = 128, message = "指标名称长度不能超过128字符")
    private String name;

    private String metricGroup;

    @Size(max = 512, message = "指标描述长度不能超过512字符")
    private String description;

    @NotBlank(message = "负责人不能为空")
    private String owner;
}
