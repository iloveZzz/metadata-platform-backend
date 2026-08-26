package com.yss.datamiddle.semantic.application.port;

/**
 * 当前用户上下文端口（yss-userinfo）。
 *
 * <p>owner / createdBy / certifiedBy / deprecatedBy / 审计操作者来源；角色复用主平台
 * （SB-08），本切片以可测 seam 落位，SL-SLICE-06 落位 RBAC 中间件后替换实现。</p>
 */
public interface CurrentUserPort {

    /**
     * 当前操作者（背景任务 / 无上下文时回退 system）。
     */
    String userName();

    /**
     * 是否具备写操作权限（治理专员 = true，只读工程师 = false）。
     */
    boolean isWritePermitted();
}
