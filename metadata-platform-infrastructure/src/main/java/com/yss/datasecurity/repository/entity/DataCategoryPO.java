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
@TableName("sec_data_category")
public class DataCategoryPO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("category_name")
    private String categoryName;

    @TableField("category_code")
    private String categoryCode;

    @TableField("tree_node_id")
    private Long treeNodeId;

    @TableField("security_grade_id")
    private Long securityGradeId;

    @TableField("priority")
    private Integer priority;

    @TableField("scan_dimension_config")
    private String scanDimensionConfig;

    @TableField("status")
    private String status;

    @TableField("disable_policy")
    private String disablePolicy;

    @TableField("description")
    private String description;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
