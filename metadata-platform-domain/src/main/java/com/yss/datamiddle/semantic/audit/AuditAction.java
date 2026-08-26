package com.yss.datamiddle.semantic.audit;

/**
 * 审计动作（audit_log.action 取值）。
 */
public enum AuditAction {

    CREATE("CREATE"),
    UPDATE("UPDATE"),
    CERTIFY("CERTIFY"),
    DEPRECATE("DEPRECATE"),
    DELETE("DELETE");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
