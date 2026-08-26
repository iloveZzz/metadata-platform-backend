package com.yss.datamiddle.aicontextlayer.domain.tool;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感字段剥离器（SEC-04 / 断言 2）。
 *
 * <p>强制剥离列注释、样例值、敏感描述等元数据，仅保留结构元数据与分级标签。</p>
 */
public class SensitiveStripper {

    public static List<AssetColumnItem> sanitizeColumns(List<AssetColumnItem> rawColumns) {
        if (rawColumns == null || rawColumns.isEmpty()) {
            return Collections.emptyList();
        }
        return rawColumns.stream()
                .map(c -> AssetColumnItem.builder()
                        .name(c.getName())
                        .dataType(c.getDataType())
                        .primaryKey(c.getPrimaryKey())
                        .nullable(c.getNullable())
                        .classificationLevel(c.getClassificationLevel())
                        .build())
                .collect(Collectors.toList());
    }
}
