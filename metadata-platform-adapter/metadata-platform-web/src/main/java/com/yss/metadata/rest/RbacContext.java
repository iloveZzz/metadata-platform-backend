package com.yss.metadata.rest;

import com.yss.metadata.domain.rbac.exception.ForbiddenException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RBAC 上下文解析（Web 层 seam；slice 06）。
 *
 * <p>在 {@link CurrentUser}（X-User-Id 用户标识）基础上扩展角色与数据域判定：
 * <ul>
 *   <li>管理员：请求头 {@code X-User-Role} 值 admin → 是；缺省 admin（PoC 兼容既有行为，平台认证接入后必带）；</li>
 *   <li>数据域：请求头 {@code X-User-Domains} 逗号分隔（网关按用户角色注入）；
 *       缺省 null = 全部数据域放行（资产列表不过滤）。</li>
 * </ul>
 * 用户-角色绑定与统一认证 seam-deferred（无用户表，平台 AuthUserInfoUtil 接入后替换解析源）。</p>
 */
public final class RbacContext {

    /** 角色请求头（值：admin / user；缺省 admin，PoC 兼容） */
    public static final String ROLE_HEADER = "X-User-Role";

    /** 数据域请求头（逗号分隔；缺省全部放行） */
    public static final String DOMAINS_HEADER = "X-User-Domains";

    /** 管理员角色值 */
    public static final String ROLE_ADMIN = "admin";

    private RbacContext() {
    }

    /**
     * 是否管理员（X-User-Role 头值 admin；缺省 admin——PoC 兼容，平台认证接入后必带）。
     */
    public static boolean isAdmin(String roleHeader) {
        return !StringUtils.hasText(roleHeader) || ROLE_ADMIN.equalsIgnoreCase(roleHeader.trim());
    }

    /**
     * 校验管理员；非管理员抛 {@link ForbiddenException}（403 rbac.forbidden）。
     */
    public static void requireAdmin(String roleHeader) {
        if (!isAdmin(roleHeader)) {
            throw new ForbiddenException("无权限：该操作仅限平台管理员（请由平台网关注入 X-User-Role=admin）");
        }
    }

    /**
     * 解析允许访问的数据域（X-User-Domains 头逗号分隔、去重）。
     *
     * <p>语义：头缺失或解析后为空 → 返回 null = 全部数据域放行（PoC 缺省兼容；
     * 平台认证接入后收敛为"空=无任何数据域权限"）。</p>
     */
    public static List<String> resolveDomains(String domainsHeader) {
        if (!StringUtils.hasText(domainsHeader)) {
            return null;
        }
        List<String> domains = new ArrayList<>();
        for (String raw : domainsHeader.split(",")) {
            String domain = raw.trim();
            if (StringUtils.hasText(domain) && !domains.contains(domain)) {
                domains.add(domain);
            }
        }
        return domains.isEmpty() ? null : Collections.unmodifiableList(domains);
    }
}
