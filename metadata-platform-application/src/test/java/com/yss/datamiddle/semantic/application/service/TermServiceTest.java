package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.TermCertifyInput;
import com.yss.datamiddle.semantic.application.model.TermCreateInput;
import com.yss.datamiddle.semantic.application.model.TermUpdateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.audit.AuditLogEntry;
import com.yss.datamiddle.semantic.audit.AuditLogGateway;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.ReferenceConflictException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import com.yss.datamiddle.semantic.term.exception.TermNameDuplicateException;
import com.yss.datamiddle.semantic.term.exception.VersionConflictException;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.gateway.TermReferenceCheckPort;
import com.yss.datamiddle.semantic.term.model.CertifyAction;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 术语写用例单测（编排：名称唯一 / 乐观锁 / 认证失效 / 删除阻断 / 审计同事务 / 只读 403 seam）。
 */
@ExtendWith(MockitoExtension.class)
class TermServiceTest {

    @Mock
    private TermGateway termGateway;
    @Mock
    private AuditLogGateway auditLogGateway;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private TermReferenceCheckPort termReferenceCheckPort;

    private TermService termService;

    @BeforeEach
    void setUp() {
        termService = new TermService(termGateway, auditLogGateway, currentUserPort,
                termReferenceCheckPort);
        when(currentUserPort.isWritePermitted()).thenReturn(true);
        when(currentUserPort.userName()).thenReturn("alice");
    }

    private TermCreateInput createInput(String name) {
        return TermCreateInput.builder().name(name).aliases(Arrays.asList("收入"))
                .definition("营业收入口径").owner("张治理").build();
    }

    private Term draftTerm(Long id) {
        Term term = Term.create("营收", Arrays.asList("收入"), "定义", null, "张治理", "alice");
        term.setId(id);
        return term;
    }

    // ---- 新建 ----

    @Test
    void createTerm_shouldSaveAndAudit() {
        when(termGateway.existsByName("营收", null)).thenReturn(false);
        Term term = termService.createTerm(createInput("营收"));
        assertEquals(TermStatus.DRAFT, term.getStatus());
        verify(termGateway).save(any(Term.class));
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    @Test
    void createTerm_duplicateName_shouldThrow422() {
        when(termGateway.existsByName("营收", null)).thenReturn(true);
        assertThrows(TermNameDuplicateException.class,
                () -> termService.createTerm(createInput("营收")));
        verify(termGateway, never()).save(any(Term.class));
    }

    @Test
    void createTerm_readOnlyUser_shouldThrow403AndWriteDeniedAudit() {
        when(currentUserPort.isWritePermitted()).thenReturn(false);
        assertThrows(PermissionDeniedException.class,
                () -> termService.createTerm(createInput("营收")));
        verify(auditLogGateway).appendDenied(any(AuditLogEntry.class));
        verify(termGateway, never()).save(any(Term.class));
    }

    // ---- 编辑与乐观锁 ----

    @Test
    void updateTerm_staleVersion_shouldThrow409WithLatest() {
        Term latest = draftTerm(1L);
        latest.setVersion(3);
        when(termGateway.findById(1L)).thenReturn(Optional.of(latest));
        TermUpdateInput input = TermUpdateInput.builder().name("营收").owner("张治理")
                .definition("定义").version(1).build();
        VersionConflictException ex = assertThrows(VersionConflictException.class,
                () -> termService.updateTerm(1L, input));
        assertEquals(latest, ex.getLatest());
        verify(termGateway, never()).updateWithVersion(any(), any(Integer.class));
    }

    @Test
    void updateTerm_success_shouldConditionalUpdateAndAudit() {
        Term term = draftTerm(1L);
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termGateway.existsByName("营收", 1L)).thenReturn(false);
        when(termGateway.updateWithVersion(any(Term.class), any(Integer.class))).thenReturn(true);

        TermUpdateInput input = TermUpdateInput.builder().name("营收").owner("张治理")
                .definition("新定义").version(0).build();
        Term updated = termService.updateTerm(1L, input);
        assertEquals("新定义", updated.getDefinition());
        assertEquals(Integer.valueOf(1), updated.getVersion());
        verify(termGateway).updateWithVersion(any(Term.class), any(Integer.class));
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    @Test
    void updateTerm_optimisticLockRace_shouldReloadAndThrow409WithLatest() {
        Term stale = draftTerm(1L);
        Term latest = draftTerm(1L);
        latest.setVersion(2);
        when(termGateway.findById(1L)).thenReturn(Optional.of(stale), Optional.of(latest));
        when(termGateway.existsByName("营收", 1L)).thenReturn(false);
        when(termGateway.updateWithVersion(any(Term.class), any(Integer.class))).thenReturn(false);

        TermUpdateInput input = TermUpdateInput.builder().name("营收").owner("张治理")
                .definition("定义").version(0).build();
        VersionConflictException ex = assertThrows(VersionConflictException.class,
                () -> termService.updateTerm(1L, input));
        assertEquals(latest, ex.getLatest());
        verify(termGateway, never()).delete(any(Term.class));
    }

    @Test
    void updateTerm_duplicateName_shouldThrow422() {
        Term term = draftTerm(1L);
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termGateway.existsByName("另一个已存在", 1L)).thenReturn(true);
        TermUpdateInput input = TermUpdateInput.builder().name("另一个已存在").owner("张治理")
                .definition("定义").version(0).build();
        assertThrows(TermNameDuplicateException.class, () -> termService.updateTerm(1L, input));
    }

    // ---- 认证 / 弃用 ----

    @Test
    void certifyTerm_draft_shouldBecomeCertifiedAndAudit() {
        Term term = draftTerm(1L);
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termGateway.updateWithVersion(any(Term.class), any(Integer.class))).thenReturn(true);

        Term result = termService.certifyTerm(1L, TermCertifyInput.builder()
                .action(CertifyAction.CERTIFY).note("初版认证").build());
        assertEquals(TermStatus.CERTIFIED, result.getStatus());
        assertEquals("alice", result.getCertifiedBy());
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    @Test
    void certifyTerm_alreadyCertified_shouldBeIdempotentWithAudit() {
        Term term = draftTerm(1L);
        term.certify("alice");
        Integer versionBefore = term.getVersion();
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));

        Term result = termService.certifyTerm(1L, TermCertifyInput.builder()
                .action(CertifyAction.CERTIFY).build());
        assertEquals(TermStatus.CERTIFIED, result.getStatus());
        assertEquals(versionBefore, result.getVersion());
        verify(termGateway, never()).updateWithVersion(any(Term.class), any(Integer.class));
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    @Test
    void certifyTerm_deprecated_shouldThrow409StateConflict() {
        Term term = draftTerm(1L);
        term.deprecate("alice");
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        assertThrows(StateConflictException.class, () -> termService.certifyTerm(1L,
                TermCertifyInput.builder().action(CertifyAction.CERTIFY).build()));
    }

    @Test
    void deprecateTerm_certified_shouldBecomeDeprecatedAndAudit() {
        Term term = draftTerm(1L);
        term.certify("alice");
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termGateway.updateWithVersion(any(Term.class), any(Integer.class))).thenReturn(true);

        Term result = termService.certifyTerm(1L, TermCertifyInput.builder()
                .action(CertifyAction.DEPRECATE).note("口径变更弃用").build());
        assertEquals(TermStatus.DEPRECATED, result.getStatus());
        assertEquals("alice", result.getDeprecatedBy());
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    // ---- 删除 ----

    @Test
    void deleteTerm_nonDraft_shouldThrow409StateConflict() {
        Term term = draftTerm(1L);
        term.certify("alice");
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        assertThrows(StateConflictException.class, () -> termService.deleteTerm(1L));
        verify(termGateway, never()).delete(any(Term.class));
    }

    @Test
    void deleteTerm_referenced_shouldThrow409ReferenceConflict() {
        Term term = draftTerm(1L);
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termReferenceCheckPort.isReferenced(1L)).thenReturn(true);
        assertThrows(ReferenceConflictException.class, () -> termService.deleteTerm(1L));
        verify(termGateway, never()).delete(any(Term.class));
    }

    @Test
    void deleteTerm_draftUnreferenced_shouldDeleteAndAudit() {
        Term term = draftTerm(1L);
        when(termGateway.findById(1L)).thenReturn(Optional.of(term));
        when(termReferenceCheckPort.isReferenced(1L)).thenReturn(false);
        termService.deleteTerm(1L);
        verify(termGateway).delete(term);
        verify(auditLogGateway).append(any(AuditLogEntry.class));
    }

    @Test
    void deleteTerm_readOnlyUser_shouldThrow403AndWriteDeniedAudit() {
        when(currentUserPort.isWritePermitted()).thenReturn(false);
        assertThrows(PermissionDeniedException.class, () -> termService.deleteTerm(1L));
        verify(auditLogGateway).appendDenied(any(AuditLogEntry.class));
        verify(termGateway, never()).findById(1L);
    }
}
