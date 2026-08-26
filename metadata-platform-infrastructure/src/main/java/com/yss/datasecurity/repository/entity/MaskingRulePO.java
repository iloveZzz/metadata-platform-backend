package com.yss.datasecurity.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sec_masking_rule", autoResultMap = true)
public class MaskingRulePO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rule_name")
    private String ruleName;

    @TableField("category_id")
    private Long categoryId;

    @TableField(exist = false)
    private String categoryName;

    @TableField("description")
    private String description;

    @TableField("algorithm_type")
    private String algorithmType;

    @TableField("sub_algorithm")
    private String subAlgorithm;

    @TableField(value = "algorithm_config", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> algorithmParams;

    @TableField("apply_scene")
    private String applyScene;

    @TableField("mask_method")
    private String maskMethod;

    @TableField("plate_scope")
    private String plateScope;

    @TableField("project_scope")
    private String projectScope;

    @TableField("scope_type")
    private String scopeType;

    @TableField(value = "scope_target", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scopeTarget;

    @TableField("key_id")
    private Long keyId;

    @TableField(exist = false)
    private String keyName;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private String status;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
