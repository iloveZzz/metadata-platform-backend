package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Data;

@Data
public class MetricCertifyCmd extends CommandDTO {
    private Boolean force = true;
}
