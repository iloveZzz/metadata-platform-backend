package com.yss.metadata.repository;

import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.infrastructure.convertor.AuditLogConvertor;
import com.yss.metadata.repository.gateway.impl.AuditLogGatewayImpl;
import com.yss.metadata.repository.entity.AuditLogPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计日志仓储 H2 持久化测试（WU-03-04 基础写入；audit_log 不可变）。
 */
class AuditLogGatewayImplH2Test extends H2MapperTestSupport {

    private AuditLogGateway auditLogGateway;
    private com.yss.metadata.repository.AuditLogRepository mapper;

    @BeforeEach
    void setUp() {
        mapper = sqlSession.getMapper(com.yss.metadata.repository.AuditLogRepository.class);
        auditLogGateway = new AuditLogGatewayImpl(mapper, Mappers.getMapper(AuditLogConvertor.class));
    }

    @Test
    @DisplayName("记录审计：操作者/动作/对象/结果/时间持久化")
    void recordAuditEntry() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 10, 30, 0);
        AuditLogEntry entry = AuditLogEntry.builder().id("audit-1").operator("u-me")
                .action("impact.export").object("task-1").result("success").time(now).build();

        auditLogGateway.record(entry);

        List<AuditLogPO> rows = mapper.selectList(null);
        assertThat(rows).hasSize(1);
        AuditLogPO po = rows.get(0);
        assertThat(po.getId()).isEqualTo("audit-1");
        assertThat(po.getOperator()).isEqualTo("u-me");
        assertThat(po.getAction()).isEqualTo("impact.export");
        assertThat(po.getObject()).isEqualTo("task-1");
        assertThat(po.getResult()).isEqualTo("success");
        assertThat(po.getTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("多次记录追加式写入（不可变语义）")
    void appendOnly() {
        auditLogGateway.record(AuditLogEntry.builder().id("a-1").operator("u-1")
                .action("lineage.manual").object("e-1").result("success").time(LocalDateTime.now()).build());
        auditLogGateway.record(AuditLogEntry.builder().id("a-2").operator("u-2")
                .action("impact.export").object("t-1").result("success").time(LocalDateTime.now()).build());

        assertThat(mapper.selectList(null)).hasSize(2);
    }

    @Test
    @DisplayName("切片 06：分页查询 time DESC + total + 空分页（只读不可变）")
    void pageTimeDesc() {
        auditLogGateway.record(AuditLogEntry.builder().id("a-1").operator("u-1")
                .action("lineage.manual").object("e-1").result("success")
                .time(LocalDateTime.of(2026, 8, 10, 9, 0, 0)).build());
        auditLogGateway.record(AuditLogEntry.builder().id("a-2").operator("u-2")
                .action("impact.export").object("t-1").result("success")
                .time(LocalDateTime.of(2026, 8, 12, 9, 0, 0)).build());
        auditLogGateway.record(AuditLogEntry.builder().id("a-3").operator("u-3")
                .action("integration.config").object("1").result("success")
                .time(LocalDateTime.of(2026, 8, 11, 9, 0, 0)).build());

        com.yss.metadata.domain.audit.model.AuditLogPage page = auditLogGateway.page(1, 10);

        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getItems()).extracting(AuditLogEntry::getId).containsExactly("a-2", "a-3", "a-1");
        assertThat(page.getItems().get(0).getAction()).isEqualTo("impact.export");
        assertThat(page.getPageIndex()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("切片 06：分页切片 + 空库空分页")
    void pageSliceAndEmpty() {
        for (int i = 1; i <= 5; i++) {
            auditLogGateway.record(AuditLogEntry.builder().id("a-" + i).operator("u-" + i)
                    .action("action." + i).object("o").result("success")
                    .time(LocalDateTime.of(2026, 8, 1, 0, 0).plusHours(i)).build());
        }

        com.yss.metadata.domain.audit.model.AuditLogPage page2 = auditLogGateway.page(2, 2);
        assertThat(page2.getTotal()).isEqualTo(5);
        assertThat(page2.getItems()).extracting(AuditLogEntry::getId).containsExactly("a-3", "a-2");

        com.yss.metadata.domain.audit.model.AuditLogPage beyond = auditLogGateway.page(99, 10);
        assertThat(beyond.getTotal()).isEqualTo(5);
        assertThat(beyond.getItems()).isEmpty();
    }
}
