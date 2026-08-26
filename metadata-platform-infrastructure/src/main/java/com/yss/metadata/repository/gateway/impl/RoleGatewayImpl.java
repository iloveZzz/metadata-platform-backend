package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.rbac.gateway.RoleGateway;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;
import com.yss.metadata.repository.DataDomainRepository;
import com.yss.metadata.repository.RoleDomainRepository;
import com.yss.metadata.repository.RoleRepository;
import com.yss.metadata.infrastructure.convertor.RoleConvertor;
import com.yss.metadata.repository.entity.DataDomainPO;
import com.yss.metadata.repository.entity.RoleDomainPO;
import com.yss.metadata.repository.entity.RolePO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色仓储实现（MyBatis-Plus；role + role_domain + data_domain）。
 *
 * <p>refs=role_domain 绑定数（列表经绑定计数组装）；data_domain 按 name
 * 幂等 upsert；删除级联清理 role_domain（data_domain 行保留）。</p>
 */
@Repository
public class RoleGatewayImpl implements RoleGateway {

    private final RoleRepository roleRepository;
    private final RoleDomainRepository roleDomainRepository;
    private final DataDomainRepository dataDomainRepository;
    private final RoleConvertor roleConvertor;

    @Autowired
    public RoleGatewayImpl(RoleRepository roleRepository,
                           RoleDomainRepository roleDomainRepository,
                           DataDomainRepository dataDomainRepository) {
        this(roleRepository, roleDomainRepository, dataDomainRepository, Mappers.getMapper(RoleConvertor.class));
    }

    public RoleGatewayImpl(RoleRepository roleRepository,
                           RoleDomainRepository roleDomainRepository,
                           DataDomainRepository dataDomainRepository,
                           RoleConvertor roleConvertor) {
        this.roleRepository = roleRepository;
        this.roleDomainRepository = roleDomainRepository;
        this.dataDomainRepository = dataDomainRepository;
        this.roleConvertor = roleConvertor != null ? roleConvertor : Mappers.getMapper(RoleConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        RolePO po = roleRepository.selectOne(Wrappers.<RolePO>lambdaQuery()
                .eq(RolePO::getName, name));
        return po == null ? Optional.empty() : Optional.of(roleConvertor.toDomain(po));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummary> listAll() {
        List<RolePO> roles = roleRepository.selectList(null);
        // refs = role_domain 绑定数（role_id → count）
        Map<String, Long> refsByRole = roleDomainRepository.selectList(null).stream()
                .collect(Collectors.groupingBy(RoleDomainPO::getRoleId, Collectors.counting()));
        List<RoleSummary> summaries = new ArrayList<>(roles.size());
        for (RolePO po : roles) {
            summaries.add(RoleSummary.builder()
                    .id(po.getId())
                    .name(po.getName())
                    .scope(po.getScope())
                    .refs(refsByRole.getOrDefault(po.getId(), 0L))
                    .build());
        }
        return summaries;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role save(Role role) {
        roleRepository.insert(roleConvertor.toPO(role));
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        roleDomainRepository.delete(Wrappers.<RoleDomainPO>lambdaQuery()
                .eq(RoleDomainPO::getRoleId, id));
        roleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public int countDomains(String roleId) {
        return Math.toIntExact(roleDomainRepository.selectCount(Wrappers.<RoleDomainPO>lambdaQuery()
                .eq(RoleDomainPO::getRoleId, roleId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceDomains(String roleId, List<String> domainNames) {
        // data_domain 幂等 upsert（by name，去重）+ role_domain 全量替换
        List<String> domainIds = new ArrayList<>();
        java.util.LinkedHashSet<String> uniqueNames = new java.util.LinkedHashSet<>();
        if (domainNames != null) {
            for (String rawName : domainNames) {
                if (rawName != null && StringUtils.hasText(rawName.trim())) {
                    uniqueNames.add(rawName.trim());
                }
            }
        }
        for (String name : uniqueNames) {
            DataDomainPO existing = dataDomainRepository.selectOne(Wrappers.<DataDomainPO>lambdaQuery()
                    .eq(DataDomainPO::getName, name));
            if (existing == null) {
                DataDomainPO created = DataDomainPO.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .build();
                dataDomainRepository.insert(created);
                domainIds.add(created.getId());
            } else {
                domainIds.add(existing.getId());
            }
        }
        roleDomainRepository.delete(Wrappers.<RoleDomainPO>lambdaQuery()
                .eq(RoleDomainPO::getRoleId, roleId));
        for (String domainId : domainIds) {
            roleDomainRepository.insert(RoleDomainPO.builder()
                    .roleId(roleId)
                    .domainId(domainId)
                    .build());
        }
    }
}
