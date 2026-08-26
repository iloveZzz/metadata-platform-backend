package com.yss.datamiddle.semantic.metric.model;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricAggregateTest {

    @Test
    @DisplayName("新建指标口径初始状态为 DRAFT 且版本为 0")
    void createInitialState() {
        MetricDefinition m = MetricDefinition.create("GMV", "gmv_group", "成交总额", "finance_owner", "u1");
        assertEquals(MetricStatus.DRAFT, m.getStatus());
        assertEquals(0, m.getCurrentVersionNo());
        assertFalse(m.getAuthoritative());
    }

    @Test
    @DisplayName("新增版本后成为当前版本，草稿自动进入 ACTIVE")
    void addVersionBecomesCurrent() {
        MetricDefinition m = MetricDefinition.create("GMV", "gmv_group", "成交总额", "finance_owner", "u1");
        MetricVersion v1 = m.addVersion("sum(order_amount)", "订单金额汇总", Arrays.asList("region"), "status='PAID'", "u1");

        assertEquals(1, m.getCurrentVersionNo());
        assertEquals(MetricStatus.ACTIVE, m.getStatus());
        assertEquals(1, v1.getVersionNo());
        assertEquals(1, m.getVersions().size());
    }

    @Test
    @DisplayName("版本回滚生成新版本并保留 rollbackFromNo 快照记录")
    void rollbackGeneratesNewVersionWithHistory() {
        MetricDefinition m = MetricDefinition.create("GMV", "gmv_group", "成交总额", "finance_owner", "u1");
        m.addVersion("sum(amount)", "v1 口径", null, null, "u1");
        m.addVersion("sum(amount) - sum(refund)", "v2 口径", null, null, "u1");
        assertEquals(2, m.getCurrentVersionNo());

        // 回滚到 v1
        MetricVersion v3 = m.rollbackTo(1, "u2");
        assertEquals(3, m.getCurrentVersionNo());
        assertEquals(3, v3.getVersionNo());
        assertEquals(1, v3.getRollbackFromNo());
        assertEquals("sum(amount)", v3.getExpression());
        assertEquals(3, m.getVersions().size());
    }

    @Test
    @DisplayName("认证冲突 SB-02: force=false 抛出 AUTH_CONFLICT，force=true 自动失效旧认证")
    void certificationConflictHandling() {
        MetricDefinition m1 = MetricDefinition.create("GMV-财务", "gmv_group", "财务口径", "finance", "u1");
        m1.setId(101L);
        m1.certify(false, null, "u1");
        assertTrue(m1.getAuthoritative());

        MetricDefinition m2 = MetricDefinition.create("GMV-运营", "gmv_group", "运营口径", "ops", "u2");
        m2.setId(102L);

        // 1. force=false 抛出冲突
        StateConflictException ex = assertThrows(StateConflictException.class, () ->
                m2.certify(false, m1, "u2")
        );
        assertTrue(ex.getMessage().contains("AUTH_CONFLICT"));
        assertTrue(m1.getAuthoritative());
        assertFalse(m2.getAuthoritative());

        // 2. force=true 抢占认证，m1 自动失效
        MetricDefinition superseded = m2.certify(true, m1, "u2");
        assertNotNull(superseded);
        assertFalse(m1.getAuthoritative());
        assertTrue(m2.getAuthoritative());
    }

    @Test
    @DisplayName("已认证口径内容变更后认证失效退回未认证")
    void updateContentInvalidatesCertification() {
        MetricDefinition m = MetricDefinition.create("GMV", "gmv_group", "成交总额", "finance_owner", "u1");
        m.certify(false, null, "u1");
        assertTrue(m.getAuthoritative());

        m.updateInfo("GMV-更新", "gmv_group", "更新说明", "new_owner", 0, "u1");
        assertFalse(m.getAuthoritative());
        assertNull(m.getCertifiedBy());
    }
}
