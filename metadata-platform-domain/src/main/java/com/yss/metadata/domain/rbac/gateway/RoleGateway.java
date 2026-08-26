package com.yss.metadata.domain.rbac.gateway;

import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓储端口（RBAC 上下文；Domain 定义，Infrastructure 实现）。
 *
 * <p>role 表 + role_domain（N:M）+ data_domain（幂等 upsert by name）：
 * 列表含 refs（role_domain 绑定数）、创建（name 唯一 + 域绑定）、
 * 删除（refs=0 前置校验由应用层执行，本端口级联清理绑定）。</p>
 */
public interface RoleGateway {

    /**
     * 按名称查询角色（name 唯一校验）。
     */
    Optional<Role> findByName(String name);

    /**
     * 角色列表（含 refs=role_domain 绑定数）。
     */
    List<RoleSummary> listAll();

    /**
     * 保存角色（新增）。
     */
    Role save(Role role);

    /**
     * 删除角色（级联清理 role_domain 绑定；data_domain 行保留，避免孤儿引用）。
     */
    void deleteById(String id);

    /**
     * 角色数据域绑定数（refs；删除前 refs=0 才可删）。
     */
    int countDomains(String roleId);

    /**
     * 替换角色数据域绑定：data_domain 幂等 upsert（by name）+ role_domain 全量替换。
     */
    void replaceDomains(String roleId, List<String> domainNames);
}
