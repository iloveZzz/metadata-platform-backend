package com.yss.datamiddle.semantic.synonym.model;

import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynonymSetAggregateTest {

    @Test
    @DisplayName("新建同义词组默认启用且必须包含主词")
    void createSynonymSetSuccess() {
        SynonymSet s = SynonymSet.create("营收同义词", "营收", Arrays.asList("营收", "营业收入", "收入"), 1L, "u1");
        assertNotNull(s);
        assertTrue(s.getEnabled());
        assertEquals("营收", s.getCanonical());
        assertEquals(3, s.getWords().size());
    }

    @Test
    @DisplayName("主词不包含在 words 中抛出 422 REQUIRED_IN_WORDS")
    void canonicalNotInWordsThrows422() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                SynonymSet.create("营收同义词", "营收", Collections.singletonList("营业收入"), 1L, "u1")
        );
        assertEquals("REQUIRED_IN_WORDS", ex.getFieldCode());
        assertEquals("canonical", ex.getField());
    }

    @Test
    @DisplayName("启停同义词组幂等生效")
    void toggleStatus() {
        SynonymSet s = SynonymSet.create("营收同义词", "营收", Arrays.asList("营收", "营业收入"), null, "u1");
        assertTrue(s.getEnabled());

        s.toggleStatus(false, "u1");
        assertFalse(s.getEnabled());

        s.toggleStatus(true, "u1");
        assertTrue(s.getEnabled());
    }
}
