package com.yss.datamiddle.smartgovernance.domain.security.model;

/**
 * 数据安全级别枚举 (L1~L5)
 * 依据：《金融数据安全分级指南 JR/T 0197-2020》与通用数据安全分级规范
 */
public enum SecurityLevel {
    L1("L1", "公开数据 (L1)", "#52c41a"),
    L2("L2", "内部数据 (L2)", "#1890ff"),
    L3("L3", "敏感数据 (L3)", "#faad14"),
    L4("L4", "高度敏感 (L4)", "#fa8c16"),
    L5("L5", "极高敏感/绝密 (L5)", "#f5222d");

    private final String code;
    private final String label;
    private final String color;

    SecurityLevel(String code, String label, String color) {
        this.code = code;
        this.label = label;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public static SecurityLevel of(String code) {
        if (code == null) {
            return L1;
        }
        for (SecurityLevel level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return L1;
    }
}
