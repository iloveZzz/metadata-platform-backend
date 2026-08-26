package com.yss.metadata.application.rbac.service;

import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.application.rbac.service.impl.RoleServiceImpl;
import com.yss.metadata.application.rbac.support.InMemoryRoleGateway;
import com.yss.metadata.client.dto.cmd.RoleCmd;
import com.yss.metadata.client.vo.RoleVO;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.rbac.exception.RoleNameConflictException;
import com.yss.metadata.domain.rbac.exception.RoleReferencedException;
import com.yss.metadata.domain.rbac.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 角色管理应用服务测试（WU-06-01）。
 *
 * <p>覆盖：列表（refs=role_domain 绑定数）、创建（name 唯一 409 + 域绑定 +
 * 审计 rbac.role.create）、删除（refs>0 → 409 role.in_use；无绑定 → 成功 +
 * 审计 rbac.role.delete）。</p>
 */
class RoleServiceTest {

    private InMemoryRoleGateway roleGateway;
    private InMemoryAuditLogRepository auditLogRepository;
    private RoleService service;

    @BeforeEach
    void setUp() {
        roleGateway = new InMemoryRoleGateway();
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new RoleServiceImpl(roleGateway, auditLogRepository, org.mapstruct.factory.Mappers.getMapper(RbacAppConvertor.class));
    }

    @Test
    @DisplayName("列表：角色含 refs（role_domain 绑定数），无绑定 refs=0")
    void listWithRefs() {
        roleGateway.seed(Role.builder().id("r-1").name("数据工程师").scope("交易域").build(), "交易域", "客户域");
        roleGateway.seed(Role.builder().id("r-2").name("只读角色").scope(null).build());

        List<RoleVO> list = service.list();

        assertThat(list).hasSize(2);
        RoleVO r1 = list.stream().filter(v -> v.getId().equals("r-1")).findFirst().get();
        assertThat(r1.getRefs()).isEqualTo(2);
        assertThat(r1.getScope()).isEqualTo("交易域");
        RoleVO r2 = list.stream().filter(v -> v.getId().equals("r-2")).findFirst().get();
        assertThat(r2.getRefs()).isZero();
    }

    @Test
    @DisplayName("创建：name 唯一 + scope + 域绑定 + 审计 rbac.role.create")
    void createBindsDomainsAndAudits() {
        RoleCmd cmd = new RoleCmd();
        cmd.setName(" 数据治理专员 ");
        cmd.setScope("交易/客户/财务域");
        cmd.setDomains(Arrays.asList("交易域", "客户域", "交易域"));

        RoleVO vo = service.create(cmd, "u-admin");

        assertThat(vo.getId()).isNotBlank();
        assertThat(vo.getName()).isEqualTo("数据治理专员");
        // 数据域绑定（去重后 2 个）
        assertThat(roleGateway.countDomains(vo.getId())).isEqualTo(2);
        // 审计
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("rbac.role.create");
        assertThat(entry.getOperator()).isEqualTo("u-admin");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
    }

    @Test
    @DisplayName("创建 name 重复：抛 409 role.name_conflict，不落库不审计")
    void createNameConflictThrows409() {
        roleGateway.seed(Role.builder().id("r-1").name("平台管理员").build());

        RoleCmd cmd = new RoleCmd();
        cmd.setName("平台管理员");

        assertThatThrownBy(() -> service.create(cmd, "u-admin"))
                .isInstanceOf(RoleNameConflictException.class);
        assertThat(roleGateway.all()).hasSize(1);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("创建 name 为空：抛非法参数（422 语义）")
    void createBlankNameThrows422() {
        RoleCmd cmd = new RoleCmd();
        cmd.setName("  ");

        assertThatThrownBy(() -> service.create(cmd, "u-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("角色名称");
    }

    @Test
    @DisplayName("删除有绑定角色：抛 409 role.in_use（引用数），不删除不审计")
    void deleteReferencedThrows409() {
        roleGateway.seed(Role.builder().id("r-1").name("数据工程师").build(), "交易域");

        assertThatThrownBy(() -> service.delete("r-1", "u-admin"))
                .isInstanceOf(RoleReferencedException.class)
                .satisfies(ex -> assertThat(((RoleReferencedException) ex).getMessage()).contains("1"));
        assertThat(roleGateway.all()).hasSize(1);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("删除无绑定角色：成功 + 审计 rbac.role.delete")
    void deleteUnreferencedSuccess() {
        roleGateway.seed(Role.builder().id("r-1").name("只读角色").build());

        service.delete("r-1", "u-admin");

        assertThat(roleGateway.all()).isEmpty();
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("rbac.role.delete");
        assertThat(entry.getObject()).isEqualTo("r-1");
    }

    @Test
    @DisplayName("创建域绑定空清单：角色可创建且 refs=0（可删除）")
    void createWithoutDomainsDeletable() {
        RoleCmd cmd = new RoleCmd();
        cmd.setName("无绑定角色");
        cmd.setDomains(Collections.emptyList());

        RoleVO vo = service.create(cmd, "u-admin");
        assertThat(roleGateway.countDomains(vo.getId())).isZero();

        service.delete(vo.getId(), "u-admin");
        assertThat(roleGateway.all()).isEmpty();
    }
}
