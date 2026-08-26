package com.yss.datamiddle.dqinsight.domain.service;

import com.yss.datamiddle.dqinsight.domain.exception.DqForbiddenException;
import com.yss.datamiddle.dqinsight.domain.gateway.DataDomainFilter;
import com.yss.datamiddle.dqinsight.domain.gateway.OperationPermissionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据域可见性与操作权限守卫领域测试（DQI-SLICE-05-WU1，C24 / DQI-007）。
 *
 * <p>域过滤语义（数据架构 §10）：可见域为空 = 不限制；domain 为 null 且受限 = 域外
 * （最小权限，不泄露）；直连域外详情抛 403 err.dq.forbidden 且错误信息不含资源标识；
 * 操作类端点无权限抛 403（能力码见 DqCapabilities）。</p>
 */
class DataDomainGuardTest {

    private DataDomainFilter dataDomainFilter;
    private OperationPermissionPort operationPermissionPort;
    private DataDomainGuard guard;

    @BeforeEach
    void setUp() {
        dataDomainFilter = mock(DataDomainFilter.class);
        operationPermissionPort = mock(OperationPermissionPort.class);
        guard = new DataDomainGuard(dataDomainFilter, operationPermissionPort);
    }

    @Test
    void emptyVisibleDomainsMeansNoRestriction() {
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.emptyList());
        assertThat(guard.visibleDomains()).isEmpty();
        assertThat(guard.canView("交易域")).isTrue();
        assertThat(guard.canView(null)).isTrue();
    }

    @Test
    void nullVisibleDomainsNormalizedToEmpty() {
        when(dataDomainFilter.visibleDomains()).thenReturn(null);
        assertThat(guard.visibleDomains()).isEmpty();
        assertThat(guard.canView("交易域")).isTrue();
    }

    @Test
    void restrictedUserSeesOnlyVisibleDomains() {
        when(dataDomainFilter.visibleDomains()).thenReturn(Arrays.asList("交易域", "风控域"));
        assertThat(guard.canView("交易域")).isTrue();
        assertThat(guard.canView("风控域")).isTrue();
        assertThat(guard.canView("数据平台域")).isFalse();
        // domain 不可判定（null）→ 域外（最小权限，不泄露存在性）
        assertThat(guard.canView(null)).isFalse();
    }

    @Test
    void assertViewAllowedThrowsForbiddenForOutOfDomain() {
        when(dataDomainFilter.visibleDomains()).thenReturn(Collections.singletonList("交易域"));
        // 域内资源通过守卫
        guard.assertViewAllowed("交易域");
        // 域外直连 403，错误信息不含资源标识（不泄露域外资源存在性）
        assertThatThrownBy(() -> guard.assertViewAllowed("数据平台域"))
                .isInstanceOf(DqForbiddenException.class)
                .hasMessageNotContaining("数据平台域");
    }

    @Test
    void operationAllowedWhenCapabilityGranted() {
        when(operationPermissionPort.canOperate("channel:create")).thenReturn(true);
        guard.assertOperationAllowed("channel:create");
        verify(operationPermissionPort).canOperate("channel:create");
    }

    @Test
    void operationDeniedThrowsForbidden() {
        when(operationPermissionPort.canOperate("audit:query")).thenReturn(false);
        assertThatThrownBy(() -> guard.assertOperationAllowed("audit:query"))
                .isInstanceOf(DqForbiddenException.class)
                .hasMessageContaining("无操作权限");
    }

    @Test
    void nullCapabilityAlwaysDenied() {
        when(operationPermissionPort.canOperate(null)).thenReturn(false);
        assertThatThrownBy(() -> guard.assertOperationAllowed(null))
                .isInstanceOf(DqForbiddenException.class);
    }

    @Test
    void visibleDomainsPassThroughToQueryFilter() {
        List<String> visible = Arrays.asList("交易域", "风控域");
        when(dataDomainFilter.visibleDomains()).thenReturn(visible);
        assertThat(guard.visibleDomains()).containsExactly("交易域", "风控域");
    }
}