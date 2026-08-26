package com.yss.datamiddle.semantic.term.gateway;

import com.yss.datamiddle.semantic.term.model.Term;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 术语分页结果（0 条以空分页表达，非错误）。
 */
@Getter
@Builder
public class TermPage {

    private final List<Term> list;

    private final long totalCount;

    private final int pageIndex;

    private final int pageSize;
}
