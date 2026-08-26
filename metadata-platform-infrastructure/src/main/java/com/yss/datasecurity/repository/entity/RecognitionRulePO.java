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
@TableName("sec_recognition_rule")
public class RecognitionRulePO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("description")
    private String description;

    @TableField("category_scope_mode")
    private String categoryScopeMode;

    @TableField("category_scope_config")
    private String categoryScopeConfig;

    @TableField("scan_source_type")
    private String scanSourceType;

    @TableField("compute_scope_config")
    private String computeScopeConfig;

    @TableField("datasource_scope_config")
    private String datasourceScopeConfig;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private String status;

    @TableField("priority")
    private Integer priority;

    @TableField("tagged_fields_count")
    private Integer taggedFieldsCount;

    @TableField("lineage_inheritance_enabled")
    private Boolean lineageInheritanceEnabled;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
