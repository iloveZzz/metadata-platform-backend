package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.term.exception.TermNotFoundException;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.gateway.TermPage;
import com.yss.datamiddle.semantic.term.gateway.TermQuery;
import com.yss.datamiddle.semantic.term.model.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 术语读用例单测（筛选 + 分页 0 条空分页 / 详情 404）。
 */
@ExtendWith(MockitoExtension.class)
class TermQueryServiceTest {

    @Mock
    private TermGateway termGateway;

    private TermQueryService termQueryService;

    @BeforeEach
    void setUp() {
        termQueryService = new TermQueryService(termGateway);
    }

    @Test
    void pageTerms_emptyResult_shouldReturnEmptyPageNotError() {
        TermQuery query = TermQuery.builder().keyword(null).status(null)
                .onlyCertified(false).pageIndex(1).pageSize(20).build();
        when(termGateway.findPage(query)).thenReturn(TermPage.builder()
                .list(Collections.emptyList()).totalCount(0).pageIndex(1).pageSize(20).build());

        TermPage page = termQueryService.pageTerms(query);
        assertEquals(0, page.getList().size());
        assertEquals(0, page.getTotalCount());
        assertEquals(1, page.getPageIndex());
        verify(termGateway).findPage(query);
    }

    @Test
    void getById_notFound_shouldThrow404() {
        when(termGateway.findById(999L)).thenReturn(Optional.empty());
        assertThrows(TermNotFoundException.class, () -> termQueryService.getById(999L));
    }
}
