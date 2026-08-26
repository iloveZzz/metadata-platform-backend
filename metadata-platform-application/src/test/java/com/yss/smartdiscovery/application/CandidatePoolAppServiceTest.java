package com.yss.smartdiscovery.application;

import com.yss.smartdiscovery.application.dto.BatchSummaryDTO;
import com.yss.smartdiscovery.application.dto.TagCandidateDTO;
import com.yss.smartdiscovery.application.service.CandidatePoolAppService;
import com.yss.smartdiscovery.domain.audit.SmartTagAuditLog;
import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.gateway.AuditLogRepository;
import com.yss.smartdiscovery.domain.gateway.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CandidatePoolAppServiceTest {

    private CandidateRepository candidateRepository;
    private AuditLogRepository auditLogRepository;
    private CandidatePoolAppService service;

    @BeforeEach
    void setUp() {
        candidateRepository = Mockito.mock(CandidateRepository.class);
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        service = new CandidatePoolAppService(candidateRepository, auditLogRepository);
    }

    @Test
    @DisplayName("批量采纳打标建议 - 写入审计日志")
    void testBatchApprove() {
        BatchSummaryDTO summary = service.batchApprove(Arrays.asList("CAN-01", "CAN-02"), "确认合理");
        assertThat(summary.getTotalProcessed()).isEqualTo(2);
        Mockito.verify(candidateRepository).updateBatchStatus(eq(Arrays.asList("CAN-01", "CAN-02")), eq("MANUAL_APPROVED"));
        Mockito.verify(auditLogRepository).save(any(SmartTagAuditLog.class));
    }

    @Test
    @DisplayName("手工修改标签值并采纳")
    void testModifyAndApprove() {
        SmartTagCandidate candidate = SmartTagCandidate.builder()
                .id("CAN-03")
                .tableName("t_cust")
                .columnName("income")
                .recommendedTagName("旧标签")
                .status("PENDING")
                .build();
        Mockito.when(candidateRepository.findById("CAN-03")).thenReturn(Optional.of(candidate));

        TagCandidateDTO result = service.modifyAndApprove("CAN-03", "财富管理客户域", "人工核实");
        assertThat(result.getRecommendedTagName()).isEqualTo("财富管理客户域");
        assertThat(result.getStatus()).isEqualTo("MANUAL_APPROVED");
    }

    @Test
    @DisplayName("按批次一键撤销回滚")
    void testRollbackBatch() {
        SmartTagAuditLog auditLog = SmartTagAuditLog.builder()
                .batchId("BATCH-001")
                .status("APPLIED")
                .build();
        Mockito.when(auditLogRepository.findByBatchId("BATCH-001")).thenReturn(Optional.of(auditLog));

        service.rollbackBatch("BATCH-001");
        assertThat(auditLog.getStatus()).isEqualTo("ROLLED_BACK");
        Mockito.verify(auditLogRepository).update(auditLog);
    }
}
