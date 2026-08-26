package com.yss.datamiddle.semantic.audit;

/**
 * 审计结果（audit_log.result 取值）。
 */
public enum AuditResult {

    SUCCESS("SUCCESS"),
    DENIED("DENIED");

    private final String code;

    AuditResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
