package com.yss.metadata.domain.rbac.exception;

import com.yss.cloud.exception.BizException;

/**
 * 无权限（403 语义，冻结 OpenAPI ForbiddenResponse；能力标识兜底）。
 *
 * <p>管理端面端点（系统管理全部端点 + 集成配置写路径）非管理员调用时抛出；
 * 由 Web 层统一映射 403。管理员判定经 RbacContext（X-User-Role 头 seam，
 * PoC 缺省 admin；平台认证接入后替换解析源）。</p>
 */
public class ForbiddenException extends BizException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super("rbac.forbidden", message);
    }
}
