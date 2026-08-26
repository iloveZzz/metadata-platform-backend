package com.yss.metadata.domain.governance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 分类规则类型（class_rule.type）。
 *
 * <p>内置规则（手机号/身份证/银行卡/邮箱）+ 自定义正则/列名/字典
 * （FR-016；交互说明「内置规则 + 自定义正则 / 列名 / 字典」）。
 * JSON 序列化/反序列化使用 value（对齐 ConnectorType 约定）。</p>
 */
public enum ClassRuleType {

    /** 内置规则（手机号/身份证/银行卡/邮箱） */
    BUILTIN("builtin", "内置规则"),

    /** 自定义正则 */
    REGEX("regex", "正则"),

    /** 列名匹配 */
    COLUMN("column", "列名"),

    /** 字典匹配 */
    DICTIONARY("dictionary", "字典");

    private final String value;

    private final String description;

    ClassRuleType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 列值 → 枚举；未知值抛非法参数（由 Web 层统一映射 422）。
     */
    @JsonCreator
    public static ClassRuleType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ClassRuleType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知分类规则类型: " + value);
    }
}
