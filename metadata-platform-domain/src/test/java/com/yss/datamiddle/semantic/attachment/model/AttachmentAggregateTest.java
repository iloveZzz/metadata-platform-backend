package com.yss.datamiddle.semantic.attachment.model;

import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttachmentAggregateTest {

    @Test
    @DisplayName("列级挂接正常创建且状态为 ACTIVE")
    void createColumnLevelAttachmentSuccess() {
        Attachment a = Attachment.create(100L, AttachmentLevel.COLUMN, "amount", SemanticObjectType.METRIC, 1L, "u1");
        assertNotNull(a);
        assertEquals(AttachmentStatus.ACTIVE, a.getStatus());
        assertEquals("amount", a.getColumnName());
    }

    @Test
    @DisplayName("列级挂接缺失 columnName 抛出 422 REQUIRED")
    void columnLevelMissingColumnNameThrows422() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                Attachment.create(100L, AttachmentLevel.COLUMN, null, SemanticObjectType.METRIC, 1L, "u1")
        );
        assertEquals("columnName", ex.getField());
        assertEquals("REQUIRED", ex.getFieldCode());
    }

    @Test
    @DisplayName("表级挂接附带 columnName 抛出 422 FORBIDDEN")
    void tableLevelWithColumnNameThrows422() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                Attachment.create(100L, AttachmentLevel.TABLE, "amount", SemanticObjectType.METRIC, 1L, "u1")
        );
        assertEquals("columnName", ex.getField());
        assertEquals("FORBIDDEN", ex.getFieldCode());
    }

    @Test
    @DisplayName("解除挂接为软删除（RELEASED）且幂等")
    void releaseAttachmentIsIdempotentSoftDelete() {
        Attachment a = Attachment.create(100L, AttachmentLevel.TABLE, null, SemanticObjectType.TERM, 1L, "u1");
        assertEquals(AttachmentStatus.ACTIVE, a.getStatus());

        a.release("u2");
        assertEquals(AttachmentStatus.RELEASED, a.getStatus());
        assertEquals("u2", a.getReleasedBy());

        // 再次解除幂等
        a.release("u3");
        assertEquals("u2", a.getReleasedBy());
    }
}
