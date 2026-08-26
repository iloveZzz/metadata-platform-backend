package com.yss.datasecurity.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sec_sensitive_rule")
public class SensitiveRulePO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_type")
    private String ruleType; // BUILTIN / CUSTOM

    @TableField("description")
    private String description;

    @TableField("priority")
    private Integer priority;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private String status;

    @TableField("category_scope_mode")
    private String categoryScopeMode;

    @TableField("category_scope_ids")
    private String categoryScopeIds; // JSON 数组

    @TableField("scan_scope_type")
    private String scanScopeType;

    @TableField("scan_scope_config")
    private String scanScopeConfig; // JSON 存储

    @TableField("feature_config")
    private String featureConfig; // JSON 存储多模态特征

    @TableField("tagged_fields_count")
    private Integer taggedFieldsCount;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
