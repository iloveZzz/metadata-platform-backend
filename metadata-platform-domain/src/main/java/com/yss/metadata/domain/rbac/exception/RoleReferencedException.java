package com.yss.metadata.domain.rbac.exception;

import com.yss.cloud.exception.BizException;

/**
 * 角色被引用无法删除（409 语义，冻结 OpenAPI DELETE /api/roles/{id} 409）。
 *
 * <p>角色存在 role_domain 数据域绑定（refs>0）时删除返回 409，避免绑定悬空；
 * 用户引用计数 seam-deferred（无用户表，slice 06 登记）。</p>
 */
public class RoleReferencedException extends BizException {

    private static final long serialVersionUID = 1L;

    public RoleReferencedException(String id, long refs) {
        super("role.in_use", "该角色仍被 " + refs + " 个数据域绑定，无法删除：" + id);
    }
}
