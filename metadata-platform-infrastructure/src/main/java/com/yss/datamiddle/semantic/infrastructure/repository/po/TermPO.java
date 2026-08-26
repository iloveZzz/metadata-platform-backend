package com.yss.datamiddle.semantic.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 术语持久化对象（term 表）。
 */
@Getter
@Setter
@TableName("term")
public class TermPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("definition")
    private String definition;

    @TableField("description")
    private String description;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private String status;

    @TableField("certified_by")
    private String certifiedBy;

    @TableField("certified_at")
    private LocalDateTime certifiedAt;

    @TableField("deprecated_by")
    private String deprecatedBy;

    @TableField("deprecated_at")
    private LocalDateTime deprecatedAt;

    @TableField("synonym_set_id")
    private Long synonymSetId;

    @TableField("version")
    private Integer version;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
