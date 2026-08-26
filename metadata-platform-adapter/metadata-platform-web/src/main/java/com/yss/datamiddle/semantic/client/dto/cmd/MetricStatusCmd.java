package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MetricStatusCmd extends CommandDTO {
    @NotBlank(message = "目标状态不能为空")
    private String status;
}
