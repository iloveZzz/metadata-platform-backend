package com.yss.datamiddle.semantic.term.model;

/**
 * 认证 / 弃用动作（冻结契约 CertifyRequest.action 枚举）。
 */
public enum CertifyAction {

    /** 认证：draft → certified（幂等，已认证返回当前状态） */
    CERTIFY("certify"),
    /** 弃用：draft / certified → deprecated（幂等，已弃用返回当前状态） */
    DEPRECATE("deprecate");

    private final String code;

    CertifyAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CertifyAction fromCode(String code) {
        for (CertifyAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未知认证动作: " + code);
    }
}
