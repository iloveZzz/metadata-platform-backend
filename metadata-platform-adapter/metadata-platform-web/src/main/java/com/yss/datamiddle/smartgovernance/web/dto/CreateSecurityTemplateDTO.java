package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateSecurityTemplateDTO implements Serializable {
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String standardAuthority;
    private String description;
    private Boolean defaultAutoApproval;
    private BigDecimal defaultThreshold;
    private List<ClassificationRuleDTO> rules;

    @Data
    public static class ClassificationRuleDTO {
        private String sensitiveType;
        private String sensitiveName;
        private String securityLevel;
        private String clauseRef;
        private String regexPattern;
        private String dictionaryWords;
        private String semanticPrompt;
        private Boolean isActive;
        private Integer priority;
    }
}
