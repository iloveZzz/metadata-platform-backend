package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.term.exception.TermNotFoundException;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.gateway.TermPage;
import com.yss.datamiddle.semantic.term.gateway.TermQuery;
import com.yss.datamiddle.semantic.term.model.Term;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 术语读用例（keyword / status / onlyCertified 筛选 + 分页；0 条以空分页表达）。
 */
@Service
@RequiredArgsConstructor
public class TermQueryService {

    private final TermGateway termGateway;

    @Transactional(readOnly = true)
    public TermPage pageTerms(TermQuery query) {
        return termGateway.findPage(query);
    }

    @Transactional(readOnly = true)
    public Term getById(Long id) {
        return termGateway.findById(id).orElseThrow(() -> new TermNotFoundException(id));
    }
}
