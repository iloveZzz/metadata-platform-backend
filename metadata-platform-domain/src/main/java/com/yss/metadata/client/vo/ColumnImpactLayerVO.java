package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 字段级影响层级分组 VO。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnImpactLayerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下游传播深度 (1, 2, 3...) */
    private Integer depth;

    /** 当前层级受波及的字段列表 */
    private List<AffectedColumnVO> affectedColumns;
}
