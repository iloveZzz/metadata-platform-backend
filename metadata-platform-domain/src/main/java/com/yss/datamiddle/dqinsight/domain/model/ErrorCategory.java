package com.yss.datamiddle.dqinsight.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 接入错误分类（冻结 OpenAPI ErrorCategory 枚举；SB-04）。
 *
 * <p>format=格式（schema 违反 / 枚举不合法 / 类型不匹配 / 必填缺失）；
 * auth=认证（凭证无效 / 过期）；network=网络（超时 / 连接失败）。</p>
 */
public enum ErrorCategory {

    /** 格式错误 */
    FORMAT("format"),
    /** 认证错误 */
    AUTH("auth"),
    /** 网络错误 */
    NETWORK("network");

    private final String code;

    ErrorCategory(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ErrorCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ErrorCategory value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static ErrorCategory fromCodeOrNull(String code) {
        return fromCode(code);
    }
}
