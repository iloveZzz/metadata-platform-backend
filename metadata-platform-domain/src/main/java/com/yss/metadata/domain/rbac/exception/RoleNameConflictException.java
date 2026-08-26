package com.yss.metadata.domain.rbac.exception;

import com.yss.cloud.exception.BizException;

/**
 * 角色名称冲突（409 语义，name 唯一）。
 */
public class RoleNameConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public RoleNameConflictException(String name) {
        super("role.name_conflict", "角色名称已存在：" + name);
    }
}
