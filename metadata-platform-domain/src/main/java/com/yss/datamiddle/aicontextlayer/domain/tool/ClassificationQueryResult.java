package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分级分类列表响应结果（契约 3.5）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ClassificationItem> items;
    private int totalCount;
}
