package com.yss.metadata.application.rbac.service;

import com.yss.metadata.client.dto.cmd.RoleCmd;
import com.yss.metadata.client.vo.RoleVO;

import java.util.List;

/**
 * 角色管理应用服务（FR-018；WU-06-01）。
 *
 * <p>GET /api/roles 列表（refs=role_domain 绑定数）；POST 创建（name 唯一
 * 409 + data_domain 幂等 upsert + role_domain 绑定 + 审计）；DELETE 删除
 * （refs>0 → 409 role.in_use；无绑定 → 204 + 审计）。</p>
 */
public interface RoleService {

    /**
     * 角色列表（含 refs）。
     */
    List<RoleVO> list();

    /**
     * 创建角色（name 唯一冲突抛 409；绑定数据域；审计 rbac.role.create）。
     */
    RoleVO create(RoleCmd cmd, String operator);

    /**
     * 删除角色（refs>0 抛 409 role.in_use；审计 rbac.role.delete）。
     */
    void delete(String id, String operator);
}
