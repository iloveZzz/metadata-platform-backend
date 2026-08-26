package com.yss.datamiddle.semantic.term.model;

/**
 * 术语状态（与冻结契约 semantic-layer.yaml Term.status 枚举一致）。
 *
 * <p>状态机：draft → certified / deprecated；certified → deprecated；deprecated 为终态。</p>
 */
public enum TermStatus {

    /** 草稿：新建 / 已认证内容变更后退回（SB-02），可编辑可删除 */
    DRAFT("draft"),
    /** 已认证：认证后可作为权威口径引用 */
    CERTIFIED("certified"),
    /** 已弃用：终态，保留展示（SB-09），不可再认证 */
    DEPRECATED("deprecated");

    private final String code;

    TermStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 由契约枚举字符串反解；无法识别时抛出 {@link IllegalArgumentException}。
     */
    public static TermStatus fromCode(String code) {
        for (TermStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知术语状态: " + code);
    }
}
