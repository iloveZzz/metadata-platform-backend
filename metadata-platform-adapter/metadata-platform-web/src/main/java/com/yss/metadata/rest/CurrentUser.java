package com.yss.metadata.rest;

/**
 * 当前用户上下文解析（Web 层 seam）。
 *
 * <p>用户标识经请求头 {@code X-User-Id} 解析，缺省
 * {@link #DEFAULT_USER}；slice 06 起角色/数据域判定收敛
 * {@link RbacContext}（X-User-Role / X-User-Domains 头），
 * 平台统一认证（AuthUserInfoUtil）接入后替换解析源。</p>
 */
public final class CurrentUser {

    /** 请求头名称（slice 06 前 seam） */
    public static final String HEADER = "X-User-Id";

    /** 缺省用户（请求头缺失时） */
    public static final String DEFAULT_USER = "default-user";

    private CurrentUser() {
    }

    /**
     * 解析当前用户：请求头 X-User-Id 非空取该值，否则返回 default-user。
     */
    public static String resolve(String headerUserId) {
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId.trim();
        }
        return DEFAULT_USER;
    }
}
