package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ReconcileMetricConflictDTO implements Serializable {
    @NotBlank(message = "权威主指标ID不能为空")
    private String canonicalIndicatorId;
    private String reconcileStrategy;
    private String comment;
}
