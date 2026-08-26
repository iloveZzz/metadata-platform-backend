package com.yss.datamiddle.smartgovernance.domain.security.model;

/**
 * 识别漏斗层级枚举
 */
public enum FunnelLayer {
    L1_REGEX("L1_REGEX", "L1 正则预筛"),
    L2_VECTOR("L2_VECTOR", "L2 语义词典与向量粗筛"),
    L3_LLM("L3_LLM", "L3 大模型上下文推理");

    private final String code;
    private final String label;

    FunnelLayer(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
