package com.yss.datamiddle.semantic.config;

import com.yss.cloud.user.AuthUserInfoUtil;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import org.springframework.stereotype.Component;

/**
 * 当前用户上下文实现（yss-userinfo）。
 *
 * <p>owner / createdBy / certifiedBy / 操作者来源：优先 AuthUserInfoUtil（请求头 / JWT
 * payload / Redis 缓存，禁止手写 JWT 解析），无上下文时回退 system。</p>
 *
 * <p>写权限：角色复用主平台 GET /api/roles（SB-08）；SL-SLICE-01 为可测 seam，缺省允许写，
 * SL-SLICE-06 落位 RBAC 中间件后按角色判定（治理专员可写 / 工程师只读）。</p>
 */
@Component
public class YssAuthCurrentUserPort implements CurrentUserPort {

    private static final String SYSTEM_USER = "system";

    @Override
    public String userName() {
        try {
            String name = AuthUserInfoUtil.userName();
            return (name == null || name.trim().isEmpty()) ? SYSTEM_USER : name.trim();
        } catch (Exception e) {
            return SYSTEM_USER;
        }
    }

    @Override
    public boolean isWritePermitted() {
        // TODO-HUMAN-REVIEW: 角色来源 = 主平台 GET /api/roles（SB-08），SL-SLICE-06 RBAC
        // 中间件落位后按角色判定；当前 seam 缺省允许写，只读场景由 CT-10 测试替身覆盖
        return true;
    }
}
