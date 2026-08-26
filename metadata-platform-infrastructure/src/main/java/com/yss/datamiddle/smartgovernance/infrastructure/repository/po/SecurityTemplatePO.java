package com.yss.datamiddle.smartgovernance.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sg_compliance_template")
public class SecurityTemplatePO {
    @TableId
    private String id;
    private String templateCode;
    private String templateName;
    private String standardAuthority;
    private String description;
    private Integer defaultAutoApproval;
    private BigDecimal defaultThreshold;
    private Integer isSystemBuiltIn;
    private Integer isActive;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
