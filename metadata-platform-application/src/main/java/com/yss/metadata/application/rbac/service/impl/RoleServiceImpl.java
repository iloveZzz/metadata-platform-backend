package com.yss.metadata.application.rbac.service.impl;

import com.yss.metadata.application.rbac.service.RoleService;
import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.client.dto.cmd.RoleCmd;
import com.yss.metadata.client.vo.RoleVO;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.rbac.exception.RoleNameConflictException;
import com.yss.metadata.domain.rbac.exception.RoleReferencedException;
import com.yss.metadata.domain.rbac.gateway.RoleGateway;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 角色管理应用服务实现（WU-06-01）。
 *
 * <p>列表（refs=role_domain 绑定数）/ 创建（name 唯一 409 + data_domain
 * 幂等 upsert + role_domain 绑定 + 审计 rbac.role.create）/ 删除
 * （refs>0 → 409 role.in_use；无绑定 → 204 + 审计 rbac.role.delete）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    /** 角色创建审计动作 */
    private static final String AUDIT_ACTION_CREATE = "rbac.role.create";

    /** 角色删除审计动作 */
    private static final String AUDIT_ACTION_DELETE = "rbac.role.delete";

    private final RoleGateway roleGateway;
    private final AuditLogGateway auditLogRepository;
    private final RbacAppConvertor rbacAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public List<RoleVO> list() {
        List<RoleSummary> summaries = roleGateway.listAll();
        return summaries.stream().map(rbacAppConvertor::toRoleVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleCmd cmd, String operator) {
        String name = cmd == null ? null : (cmd.getName() == null ? null : cmd.getName().trim());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (roleGateway.findByName(name).isPresent()) {
            throw new RoleNameConflictException(name);
        }
        Role role = roleGateway.save(Role.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .scope(trimToNull(cmd.getScope()))
                .build());
        roleGateway.replaceDomains(role.getId(), cmd.getDomains());

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_CREATE)
                .object(role.getId())
                .result("success")
                .time(java.time.LocalDateTime.now())
                .build());
        log.info("角色已创建，roleId={}, name={}, operator={}", role.getId(), name, operator);

        // 复用 RbacAppConvertor（MapStruct）组装 VO
        return rbacAppConvertor.toRoleVO(RoleSummary.builder()
                .id(role.getId())
                .name(name)
                .scope(role.getScope())
                .refs(roleGateway.countDomains(role.getId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String operator) {
        int refs = roleGateway.countDomains(id);
        if (refs > 0) {
            throw new RoleReferencedException(id, refs);
        }
        roleGateway.deleteById(id);
        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_DELETE)
                .object(id)
                .result("success")
                .time(java.time.LocalDateTime.now())
                .build());
        log.info("角色已删除，roleId={}, operator={}", id, operator);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
