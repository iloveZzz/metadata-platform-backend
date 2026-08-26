package com.yss.metadata.rest;

import com.yss.metadata.application.rbac.service.AuditQueryService;
import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.application.rbac.service.impl.AuditQueryServiceImpl;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审计日志查询 REST 契约测试（WU-06-04，冻结 OpenAPI /api/audit-logs 段）。
 *
 * <p>覆盖：分页 200（time DESC PageResult）/空分页/非管理员 403。</p>
 */
class AuditLogControllerTest {

    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        auditLogRepository = new InMemoryAuditLogRepository();
        AuditQueryService queryService = new AuditQueryServiceImpl(auditLogRepository, org.mapstruct.factory.Mappers.getMapper(RbacAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(queryService))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/audit-logs 管理员：200 PageResult（time DESC + totalCount）")
    void pageReturns200() throws Exception {
        auditLogRepository.record(entry("a-1", "u-1", "lineage.manual", LocalDateTime.of(2026, 8, 10, 9, 0)));
        auditLogRepository.record(entry("a-2", "u-2", "impact.export", LocalDateTime.of(2026, 8, 12, 9, 0)));
        auditLogRepository.record(entry("a-3", "u-3", "integration.config", LocalDateTime.of(2026, 8, 11, 9, 0)));

        mockMvc.perform(get("/api/audit-logs").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].action").value("impact.export"))
                .andExpect(jsonPath("$.data[1].action").value("integration.config"))
                .andExpect(jsonPath("$.data[2].action").value("lineage.manual"))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageIndex").value(1));
    }

    @Test
    @DisplayName("GET /api/audit-logs 空库：200 空分页（非错误）")
    void emptyPageReturns200() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    @DisplayName("GET /api/audit-logs 非管理员：403 rbac.forbidden")
    void nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs").header("X-User-Role", "user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("rbac.forbidden"));
    }

    private AuditLogEntry entry(String id, String operator, String action, LocalDateTime time) {
        return AuditLogEntry.builder()
                .id(id).operator(operator).action(action)
                .object("obj-" + id).result("success").time(time)
                .build();
    }
}
