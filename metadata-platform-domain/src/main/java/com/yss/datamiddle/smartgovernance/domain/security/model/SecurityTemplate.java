package com.yss.datamiddle.smartgovernance.domain.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行业合规模板 (聚合根)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateCode;
    private String templateName;
    private String standardAuthority;
    private String description;
    private Boolean defaultAutoApproval;
    private BigDecimal defaultThreshold;
    private Boolean isSystemBuiltIn;
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ClassificationRule> rules;
}
