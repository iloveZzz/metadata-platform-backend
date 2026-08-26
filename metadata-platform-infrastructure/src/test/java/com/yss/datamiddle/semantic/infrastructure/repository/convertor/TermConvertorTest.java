package com.yss.datamiddle.semantic.infrastructure.repository.convertor;

import com.yss.datamiddle.semantic.audit.AuditAction;
import com.yss.datamiddle.semantic.audit.AuditLogEntry;
import com.yss.datamiddle.semantic.audit.AuditResult;
import com.yss.datamiddle.semantic.infrastructure.repository.po.AuditLogPO;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermAliasPO;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermPO;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 基础设施 Convertor 单测（MapStruct：PO ↔ 领域模型，状态码 / 别名装配）。
 */
class TermConvertorTest {

    private final TermConvertor convertor = new TermConvertorImpl();

    @Test
    void toDomain_shouldMapStatusCodeAndBaseFields() {
        TermPO po = new TermPO();
        po.setId(1L);
        po.setName("营收");
        po.setStatus("certified");
        po.setOwner("张治理");
        po.setCertifiedBy("alice");
        po.setVersion(2);

        Term term = convertor.toDomain(po);
        assertEquals(Long.valueOf(1L), term.getId());
        assertEquals("营收", term.getName());
        assertEquals(TermStatus.CERTIFIED, term.getStatus());
        assertEquals("alice", term.getCertifiedBy());
        assertEquals(Integer.valueOf(2), term.getVersion());
    }

    @Test
    void toPO_shouldMapEnumToCodeString() {
        Term term = Term.create("营收", Arrays.asList("收入"), "定义", null, "张治理", "alice");
        term.setId(1L);
        term.certify("alice");

        TermPO po = convertor.toPO(term);
        assertEquals("营收", po.getName());
        assertEquals("certified", po.getStatus());
        assertEquals("alice", po.getCertifiedBy());
    }

    @Test
    void toDomain_nullStatus_shouldBeNull() {
        TermPO po = new TermPO();
        po.setStatus(null);
        assertNull(convertor.toDomain(po).getStatus());
    }

    @Test
    void toAliasPOList_shouldBuildRowsWithTermId() {
        List<TermAliasPO> rows = convertor.toAliasPOList(7L, Arrays.asList("收入", "Revenue"));
        assertEquals(2, rows.size());
        assertEquals(Long.valueOf(7L), rows.get(0).getTermId());
        assertEquals("收入", rows.get(0).getAlias());
        assertEquals("Revenue", rows.get(1).getAlias());
    }

    @Test
    void toAliasList_shouldExtractAliasStrings() {
        TermAliasPO po1 = new TermAliasPO();
        po1.setAlias("收入");
        TermAliasPO po2 = new TermAliasPO();
        po2.setAlias("Revenue");
        assertEquals(Arrays.asList("收入", "Revenue"), convertor.toAliasList(Arrays.asList(po1, po2)));
    }

    @Test
    void auditConvertor_shouldMapEntryToPO() {
        AuditLogEntry entry = AuditLogEntry.of("alice", AuditAction.CERTIFY, "term", 1L,
                "初版认证", AuditResult.SUCCESS);
        AuditLogPO po = new SemanticAuditLogConvertorImpl().toPO(entry);
        assertEquals("alice", po.getOperator());
        assertEquals("CERTIFY", po.getAction());
        assertEquals("term", po.getObjectType());
        assertEquals(Long.valueOf(1L), po.getObjectId());
        assertEquals("SUCCESS", po.getResult());
    }
}
