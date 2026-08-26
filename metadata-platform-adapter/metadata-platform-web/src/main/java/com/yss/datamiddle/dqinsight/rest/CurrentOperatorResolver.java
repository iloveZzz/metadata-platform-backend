package com.yss.datamiddle.dqinsight.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前操作者解析（审计 operator MVP，切片 05）。
 *
 * <p>yss-userinfo starter 未入脚手架（切片 04 人工审查点），MVP 从传播用户头
 * {@code X-Username}（对齐 yss-userinfo 头约定）读取操作者；缺失时回退
 * {@code system}（与切片 04 先例一致）。真实 RBAC 接入后以 yss-userinfo 的
 * AuthUserInfoUtil 替换（人工审查点，OQ-05）。</p>
 */
@Component
@RequiredArgsConstructor
public class CurrentOperatorResolver {

    /** 传播用户头（yss-userinfo 约定：X-Username） */
    public static final String USERNAME_HEADER = "X-Username";

    /** 无用户上下文时的审计操作者（切片 04 先例：system） */
    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 当前操作者（X-Username 头 → 回退 system；无请求上下文 = system）。
     */
    public String currentOperator() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String header = request.getHeader(USERNAME_HEADER);
            if (header != null && !header.trim().isEmpty()) {
                return header.trim();
            }
        }
        return SYSTEM_OPERATOR;
    }
}
