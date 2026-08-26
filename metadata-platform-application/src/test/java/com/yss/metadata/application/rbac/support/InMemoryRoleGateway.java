package com.yss.metadata.application.rbac.support;

import com.yss.metadata.domain.rbac.gateway.RoleGateway;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色仓储内存实现（应用/契约测试 seam；镜像 refs=role_domain 绑定数语义）。
 */
public class InMemoryRoleGateway implements RoleGateway {

    private final List<Role> roles = new ArrayList<>();

    /** roleId → 绑定域名清单 */
    private final java.util.Map<String, List<String>> domainBindings = new java.util.LinkedHashMap<>();

    public void seed(Role role, String... domains) {
        roles.add(role);
        domainBindings.put(role.getId(), new ArrayList<>(java.util.Arrays.asList(domains)));
    }

    public List<Role> all() {
        return Collections.unmodifiableList(roles);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roles.stream().filter(role -> Objects.equals(role.getName(), name)).findFirst();
    }

    @Override
    public List<RoleSummary> listAll() {
        return roles.stream().map(role -> RoleSummary.builder()
                .id(role.getId())
                .name(role.getName())
                .scope(role.getScope())
                .refs(domainBindings.getOrDefault(role.getId(), Collections.emptyList()).size())
                .build()).collect(Collectors.toList());
    }

    @Override
    public Role save(Role role) {
        roles.removeIf(existing -> existing.getId().equals(role.getId()));
        roles.add(role);
        return role;
    }

    @Override
    public void deleteById(String id) {
        roles.removeIf(role -> role.getId().equals(id));
        domainBindings.remove(id);
    }

    @Override
    public int countDomains(String roleId) {
        return domainBindings.getOrDefault(roleId, Collections.emptyList()).size();
    }

    @Override
    public void replaceDomains(String roleId, List<String> domainNames) {
        List<String> cleaned = domainNames == null ? new ArrayList<>()
                : domainNames.stream().filter(Objects::nonNull)
                        .map(String::trim).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
        domainBindings.put(roleId, cleaned);
    }
}
