package com.yss.metadata.repository.entity;

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

/**
 * 分级分类结果持久化对象（classification 表；WU-04-02）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("classification")
public class ClassificationPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("asset_id")
    private String assetId;

    @TableField("column_id")
    private String columnId;

    @TableField("name")
    private String name;

    @TableField("level")
    private String level;

    @TableField("source")
    private String source;

    @TableField("status")
    private String status;
}
