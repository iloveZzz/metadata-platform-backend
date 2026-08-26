package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 识别规则与分类特征过滤维度枚举
 */
@Getter
@RequiredArgsConstructor
public enum FilterDimensionEnum {
    TABLE_NAME("TABLE_NAME", "表全名"),
    TABLE_COMMENT("TABLE_COMMENT", "表描述"),
    DB_SCHEMA("DB_SCHEMA", "库/Schema"),
    ASSET_TAG("ASSET_TAG", "资产标签"),
    COLUMN_NAME("COLUMN_NAME", "列名"),
    COLUMN_COMMENT("COLUMN_COMMENT", "列注释"),
    DATA_TYPE("DATA_TYPE", "字段数据类型"),
    CONTENT("CONTENT", "数据样本内容");

    private final String code;
    private final String description;

    public static FilterDimensionEnum of(String code) {
        for (FilterDimensionEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return COLUMN_NAME;
    }
}
