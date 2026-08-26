package com.yss.metadata.domain.governance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 待识别列（识别引擎输入：资产列名/注释；列级分类候选）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognizableColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 所属资产 id */
    private String assetId;

    /** 列 id（识别候选挂载列） */
    private String columnId;

    /** 列名 */
    private String name;

    /** 列注释 */
    private String comment;
}
