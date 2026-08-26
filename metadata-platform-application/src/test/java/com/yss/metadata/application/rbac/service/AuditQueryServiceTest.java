package com.yss.metadata.application.rbac.service;

import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.application.rbac.service.impl.AuditQueryServiceImpl;
import com.yss.metadata.client.vo.AuditLogVO;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计查询应用服务测试（WU-06-02；只读不可变，time DESC 分页）。
 */
class AuditQueryServiceTest {

    private InMemoryAuditLogRepository auditLogRepository;
    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new AuditQueryServiceImpl(auditLogRepository, org.mapstruct.factory.Mappers.getMapper(RbacAppConvertor.class));
    }

    @Test
    @DisplayName("分页查询：time DESC 排序 + total + 字段映射")
    void pageTimeDesc() {
        auditLogRepository.record(entry("a-1", "u-1", "lineage.manual", LocalDateTime.of(2026, 8, 10, 9, 0)));
        auditLogRepository.record(entry("a-2", "u-2", "impact.export", LocalDateTime.of(2026, 8, 12, 9, 0)));
        auditLogRepository.record(entry("a-3", "u-3", "integration.config", LocalDateTime.of(2026, 8, 11, 9, 0)));

        AuditQueryService.AuditPage page = service.page(1, 10);

        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getItems()).extracting(AuditLogVO::getId).containsExactly("a-2", "a-3", "a-1");
        AuditLogVO first = page.getItems().get(0);
        assertThat(first.getAction()).isEqualTo("impact.export");
        assertThat(first.getOperator()).isEqualTo("u-2");
        assertThat(first.getTime()).isEqualTo(LocalDateTime.of(2026, 8, 12, 9, 0));
    }

    @Test
    @DisplayName("分页切片：page=2 size=2 返回剩余 1 条")
    void pageSlice() {
        for (int i = 1; i <= 5; i++) {
            auditLogRepository.record(entry("a-" + i, "u-" + i, "action." + i,
                    LocalDateTime.of(2026, 8, 1, 0, 0).plusHours(i)));
        }

        AuditQueryService.AuditPage page = service.page(2, 2);

        assertThat(page.getTotal()).isEqualTo(5);
        // time DESC：a-5, a-4 | a-3, a-2 | a-1
        assertThat(page.getItems()).extracting(AuditLogVO::getId).containsExactly("a-3", "a-2");
    }

    @Test
    @DisplayName("空库分页：空 items + total=0（空分页非错误）")
    void emptyPage() {
        AuditQueryService.AuditPage page = service.page(1, 20);

        assertThat(page.getTotal()).isZero();
        assertThat(page.getItems()).isEmpty();
    }

    private AuditLogEntry entry(String id, String operator, String action, LocalDateTime time) {
        return AuditLogEntry.builder()
                .id(id).operator(operator).action(action)
                .object("obj-" + id).result("success").time(time)
                .build();
    }
}
