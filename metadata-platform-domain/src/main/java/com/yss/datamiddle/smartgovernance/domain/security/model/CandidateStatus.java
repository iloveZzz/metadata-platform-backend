package com.yss.datamiddle.smartgovernance.domain.security.model;

/**
 * 安全打标候选状态枚举
 */
public enum CandidateStatus {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已采纳生效"),
    MODIFIED("MODIFIED", "已修正生效"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String label;

    CandidateStatus(String code, String label) {
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
