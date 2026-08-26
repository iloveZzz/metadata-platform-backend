package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSecurityTemplateDTO implements Serializable {
    private String templateName;
    private String description;
    private Boolean defaultAutoApproval;
    private BigDecimal defaultThreshold;
    private Boolean isActive;
}
