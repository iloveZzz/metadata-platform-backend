package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.QueryExpansionResult;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SearchExpansionServiceTest {

    private final SynonymSetGateway gateway = Mockito.mock(SynonymSetGateway.class);
    private final SearchExpansionService service = new SearchExpansionService(gateway);

    @Test
    @DisplayName("SB-05: 仅启用组参与检索扩展，停用组不返回")
    void onlyActiveSetsParticipateInExpansion() {
        SynonymSet activeSet = SynonymSet.create("营收组", "营收", Arrays.asList("营收", "营业收入"), 1L, "u1");
        activeSet.setId(10L);

        SynonymSet disabledSet = SynonymSet.create("利润组", "利润", Arrays.asList("利润", "净利润"), 2L, "u1");
        disabledSet.setId(20L);
        disabledSet.setEnabled(false);

        when(gateway.listAll()).thenReturn(Arrays.asList(activeSet, disabledSet));

        List<QueryExpansionResult> results = service.expand(Arrays.asList("营收", "利润", "无匹配"));
        assertNotNull(results);
        assertEquals(3, results.size());

        // 1. 营收命中启用组
        assertEquals("营收", results.get(0).getQuery());
        assertEquals(1, results.get(0).getExpansions().size());
        assertEquals("营收", results.get(0).getExpansions().get(0).getCanonical());

        // 2. 利润为停用组，扩展为空
        assertEquals("利润", results.get(1).getQuery());
        assertTrue(results.get(1).getExpansions().isEmpty());

        // 3. 无匹配词扩展为空
        assertEquals("无匹配", results.get(2).getQuery());
        assertTrue(results.get(2).getExpansions().isEmpty());
    }
}
