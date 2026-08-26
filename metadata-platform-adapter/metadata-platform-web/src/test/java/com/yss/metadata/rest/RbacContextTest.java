package com.yss.metadata.rest;

import com.yss.metadata.domain.rbac.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RBAC 上下文解析测试（WU-06-03，合同 expected_evidence）。
 *
 * <p>覆盖：isAdmin（缺省 admin / 头大小写不敏感 / user 非管理员）、
 * requireAdmin 403、resolveDomains（缺省全部放行 / 逗号分隔去重 / 空清单 null）。</p>
 */
class RbacContextTest {

    @Test
    @DisplayName("isAdmin：缺省（无头）与 admin/ADMIN 均为管理员；user 非管理员")
    void isAdminDefaultsToAdmin() {
        assertThat(RbacContext.isAdmin(null)).isTrue();
        assertThat(RbacContext.isAdmin("")).isTrue();
        assertThat(RbacContext.isAdmin("  ")).isTrue();
        assertThat(RbacContext.isAdmin("admin")).isTrue();
        assertThat(RbacContext.isAdmin("ADMIN")).isTrue();
        assertThat(RbacContext.isAdmin("Admin")).isTrue();
        assertThat(RbacContext.isAdmin("user")).isFalse();
    }

    @Test
    @DisplayName("requireAdmin：非管理员抛 ForbiddenException（403 rbac.forbidden）")
    void requireAdminRejectsNonAdmin() {
        assertThatThrownBy(() -> RbacContext.requireAdmin("user"))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((ForbiddenException) ex).getErrCode()).isEqualTo("rbac.forbidden"));
        // 缺省/管理员放行
        RbacContext.requireAdmin(null);
        RbacContext.requireAdmin("admin");
    }

    @Test
    @DisplayName("resolveDomains：缺省 null=全部放行；逗号分隔去重；空清单/纯逗号 null")
    void resolveDomainsParsing() {
        assertThat(RbacContext.resolveDomains(null)).isNull();
        assertThat(RbacContext.resolveDomains("")).isNull();

        List<String> domains = RbacContext.resolveDomains("交易域, 客户域 ,交易域");
        assertThat(domains).containsExactly("交易域", "客户域");
        // 去重且保持顺序
        assertThat(domains).doesNotHaveDuplicates();

        // 仅空白/纯逗号 → null（全部放行语义）
        assertThat(RbacContext.resolveDomains(" , ")).isNull();
        assertThat(RbacContext.resolveDomains(" ")).isNull();
    }
}
