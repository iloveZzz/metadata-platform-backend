package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 已入库列引用（saveAssets 返回；敏感识别候选挂载列 id 用）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedColumnRef implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 已入库列 id */
    private String columnId;

    /** 列名 */
    private String name;

    /** 列注释（识别输入） */
    private String comment;
}
