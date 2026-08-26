package com.yss.datamiddle.semantic.infrastructure.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 术语别名持久化对象（term_alias 表，与 term 同属一个聚合）。
 */
@Getter
@Setter
@TableName("term_alias")
public class TermAliasPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("term_id")
    private Long termId;

    @TableField("alias")
    private String alias;
}
