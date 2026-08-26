package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MarkMetricConflictSuspectDTO implements Serializable {
    @NotBlank(message = "存疑原因说明不能为空")
    private String reason;
}
