package com.yss.metadata.domain.dq.model;

/**
 * 资产数据存疑流转状态枚举
 *
 * @author ai
 * @since 2026-08-15
 */
public enum TaintStatus {
    /** 正常（未标记或已解除） */
    NORMAL,

    /** 存疑（质量异常或受上游根因故障扩散） */
    TAINTED;

    public static boolean isValid(String status) {
        if (status == null) return false;
        try {
            valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
