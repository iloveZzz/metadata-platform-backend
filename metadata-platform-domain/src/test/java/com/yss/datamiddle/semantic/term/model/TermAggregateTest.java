package com.yss.datamiddle.semantic.term.model;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 术语聚合领域单测：状态机（draft→certified/deprecated）、认证失效回退（SB-02）、
 * 幂等、删除语义。
 */
class TermAggregateTest {

    private Term newDraft() {
        return Term.create("营收", Arrays.asList("收入", "Revenue"), "营业收入口径",
                "主营业务收入", "张治理", "alice");
    }

    @Test
    void create_shouldBeDraftWithVersionZero() {
        Term term = newDraft();
        assertEquals(TermStatus.DRAFT, term.getStatus());
        assertEquals(Integer.valueOf(0), term.getVersion());
        assertEquals("alice", term.getCreatedBy());
        assertEquals("张治理", term.getOwner());
        assertEquals(Arrays.asList("收入", "Revenue"), term.getAliases());
    }

    @Test
    void certifyDraft_shouldMoveToCertified() {
        Term term = newDraft();
        term.certify("alice");
        assertEquals(TermStatus.CERTIFIED, term.getStatus());
        assertEquals("alice", term.getCertifiedBy());
        assertTrue(term.getCertifiedAt() != null);
        assertEquals(Integer.valueOf(1), term.getVersion());
    }

    @Test
    void certifyAlreadyCertified_shouldBeIdempotent() {
        Term term = newDraft();
        term.certify("alice");
        Integer versionAfterFirst = term.getVersion();
        String certifiedByAfterFirst = term.getCertifiedBy();

        term.certify("bob");
        assertEquals(TermStatus.CERTIFIED, term.getStatus());
        assertEquals(versionAfterFirst, term.getVersion());
        assertEquals(certifiedByAfterFirst, term.getCertifiedBy());
    }

    @Test
    void deprecateDraft_shouldMoveToDeprecated() {
        Term term = newDraft();
        term.deprecate("alice");
        assertEquals(TermStatus.DEPRECATED, term.getStatus());
        assertEquals("alice", term.getDeprecatedBy());
        assertTrue(term.getDeprecatedAt() != null);
    }

    @Test
    void deprecateCertified_shouldMoveToDeprecatedAndKeepCertifyHistory() {
        Term term = newDraft();
        term.certify("alice");
        term.deprecate("bob");
        assertEquals(TermStatus.DEPRECATED, term.getStatus());
        assertEquals("bob", term.getDeprecatedBy());
        // SB-09：弃用保留认证信息（历史可回溯）
        assertEquals("alice", term.getCertifiedBy());
    }

    @Test
    void deprecateAlreadyDeprecated_shouldBeIdempotent() {
        Term term = newDraft();
        term.deprecate("alice");
        Integer versionAfterFirst = term.getVersion();

        term.deprecate("bob");
        assertEquals(TermStatus.DEPRECATED, term.getStatus());
        assertEquals(versionAfterFirst, term.getVersion());
        assertEquals("alice", term.getDeprecatedBy());
    }

    @Test
    void certifyDeprecated_shouldThrowStateConflict() {
        Term term = newDraft();
        term.deprecate("alice");
        assertThrows(StateConflictException.class, () -> term.certify("bob"));
    }

    @Test
    void updateContent_certified_shouldFallBackToDraftAndClearCertification() {
        Term term = newDraft();
        term.certify("alice");
        assertEquals(TermStatus.CERTIFIED, term.getStatus());

        term.updateContent("营收（修订）", Arrays.asList("收入"), "修订后定义", null, "李治理");
        assertEquals(TermStatus.DRAFT, term.getStatus());
        assertNull(term.getCertifiedBy());
        assertNull(term.getCertifiedAt());
        assertEquals("营收（修订）", term.getName());
        assertEquals(Integer.valueOf(2), term.getVersion());
    }

    @Test
    void updateContent_draft_shouldStayDraftAndIncrementVersion() {
        Term term = newDraft();
        Integer versionBefore = term.getVersion();
        term.updateContent("营收", Arrays.asList("收入"), "新定义", "描述", "王治理");
        assertEquals(TermStatus.DRAFT, term.getStatus());
        assertEquals(Integer.valueOf(versionBefore + 1), term.getVersion());
        assertEquals("新定义", term.getDefinition());
    }

    @Test
    void assertDeletable_draft_shouldPass() {
        Term term = newDraft();
        term.assertDeletable();
    }

    @Test
    void assertDeletable_certified_shouldThrowStateConflict() {
        Term term = newDraft();
        term.certify("alice");
        assertThrows(StateConflictException.class, term::assertDeletable);
    }

    @Test
    void assertDeletable_deprecated_shouldThrowStateConflict() {
        Term term = newDraft();
        term.deprecate("alice");
        assertThrows(StateConflictException.class, term::assertDeletable);
    }

    @Test
    void updateContent_shouldNotMutateCallerAliasList() {
        Term term = Term.create("营收", Arrays.asList("收入"), "定义", null, "owner", "alice");
        term.updateContent("营收", Arrays.asList("新别名"), "定义", null, "owner");
        assertEquals(1, term.getAliases().size());
    }
}
