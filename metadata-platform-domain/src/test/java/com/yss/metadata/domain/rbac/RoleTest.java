package com.yss.metadata.domain.rbac;

import com.yss.metadata.domain.rbac.exception.ForbiddenException;
import com.yss.metadata.domain.rbac.exception.RoleNameConflictException;
import com.yss.metadata.domain.rbac.exception.RoleReferencedException;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 角色领域模型测试（WU-06-01）。
 *
 * <p>覆盖：角色/摘要模型承载、refs 语义（role_domain 绑定数）、
 * 异常契约（409 name 冲突 / 409 引用 / 403 无权限）。</p>
 */
class RoleTest {

    @Test
    @DisplayName("角色模型承载：id/name/scope")
    void roleModel() {
        Role role = Role.builder().id("r-1").name("数据工程师").scope("交易/客户/财务域").build();

        assertThat(role.getId()).isEqualTo("r-1");
        assertThat(role.getName()).isEqualTo("数据工程师");
        assertThat(role.getScope()).isEqualTo("交易/客户/财务域");
    }

    @Test
    @DisplayName("角色摘要承载 refs（引用数）")
    void roleSummaryCarriesRefs() {
        RoleSummary summary = RoleSummary.builder()
                .id("r-1").name("数据治理专员").scope("全部数据域").refs(3).build();

        assertThat(summary.getRefs()).isEqualTo(3);
        assertThat(summary.getName()).isEqualTo("数据治理专员");
    }

    @Test
    @DisplayName("名称冲突异常：409 语义 code role.name_conflict")
    void nameConflictExceptionContract() {
        assertThatThrownBy(() -> {
            throw new RoleNameConflictException("平台管理员");
        })
                .isInstanceOf(RoleNameConflictException.class)
                .satisfies(ex -> {
                    RoleNameConflictException e = (RoleNameConflictException) ex;
                    assertThat(e.getErrCode()).isEqualTo("role.name_conflict");
                    assertThat(e.getMessage()).contains("平台管理员");
                });
    }

    @Test
    @DisplayName("引用冲突异常：409 语义 code role.in_use + 携带引用数")
    void referencedExceptionContract() {
        assertThatThrownBy(() -> {
            throw new RoleReferencedException("r-1", 3);
        })
                .isInstanceOf(RoleReferencedException.class)
                .satisfies(ex -> {
                    RoleReferencedException e = (RoleReferencedException) ex;
                    assertThat(e.getErrCode()).isEqualTo("role.in_use");
                    assertThat(e.getMessage()).contains("3");
                });
    }

    @Test
    @DisplayName("无权限异常：403 语义 code rbac.forbidden")
    void forbiddenExceptionContract() {
        assertThatThrownBy(() -> {
            throw new ForbiddenException("无权限");
        })
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> {
                    ForbiddenException e = (ForbiddenException) ex;
                    assertThat(e.getErrCode()).isEqualTo("rbac.forbidden");
                    assertThat(e.getMessage()).contains("无权限");
                });
    }
}
