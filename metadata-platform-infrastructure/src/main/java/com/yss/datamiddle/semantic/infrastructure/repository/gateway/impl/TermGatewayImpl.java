package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.semantic.infrastructure.repository.convertor.TermConvertor;
import com.yss.datamiddle.semantic.infrastructure.repository.mapper.TermAliasMapper;
import com.yss.datamiddle.semantic.infrastructure.repository.mapper.TermMapper;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermAliasPO;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermPO;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.gateway.TermPage;
import com.yss.datamiddle.semantic.term.gateway.TermQuery;
import com.yss.datamiddle.semantic.term.model.Term;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 术语持久化网关实现（term + term_alias 单聚合，事务由 Application 用例边界控制）。
 */
@Repository
@RequiredArgsConstructor
public class TermGatewayImpl implements TermGateway {

    private final TermMapper termMapper;
    private final TermAliasMapper termAliasMapper;
    private final TermConvertor termConvertor;

    @Override
    public Optional<Term> findById(Long id) {
        TermPO po = termMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(assemble(po));
    }

    @Override
    public Optional<Term> findByName(String name) {
        TermPO po = termMapper.selectOne(Wrappers.lambdaQuery(TermPO.class)
                .eq(TermPO::getName, name)
                .last("LIMIT 1"));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(assemble(po));
    }

    @Override
    public boolean existsByName(String name, Long excludeId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TermPO> wrapper =
                Wrappers.lambdaQuery(TermPO.class).eq(TermPO::getName, name);
        if (excludeId != null) {
            wrapper.ne(TermPO::getId, excludeId);
        }
        return termMapper.selectCount(wrapper) > 0;
    }

    @Override
    public void save(Term term) {
        TermPO po = termConvertor.toPO(term);
        termMapper.insert(po);
        term.setId(po.getId());
        insertAliases(po.getId(), term.getAliases());
    }

    @Override
    public boolean updateWithVersion(Term term, int expectedVersion) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TermPO> wrapper =
                Wrappers.lambdaUpdate(TermPO.class)
                        .eq(TermPO::getId, term.getId())
                        .eq(TermPO::getVersion, expectedVersion);
        wrapper.set(TermPO::getName, term.getName())
                .set(TermPO::getDefinition, term.getDefinition())
                .set(TermPO::getDescription, term.getDescription())
                .set(TermPO::getOwner, term.getOwner())
                .set(TermPO::getStatus, term.getStatus().getCode())
                .set(TermPO::getCertifiedBy, term.getCertifiedBy())
                .set(TermPO::getCertifiedAt, term.getCertifiedAt())
                .set(TermPO::getDeprecatedBy, term.getDeprecatedBy())
                .set(TermPO::getDeprecatedAt, term.getDeprecatedAt())
                .set(TermPO::getVersion, term.getVersion())
                .set(TermPO::getUpdatedAt, term.getUpdatedAt());
        int rows = termMapper.update(null, wrapper);
        if (rows == 0) {
            // 条件 UPDATE（WHERE version=?）匹配 0 行：版本过期或已删除
            return false;
        }
        replaceAliases(term.getId(), term.getAliases());
        return true;
    }

    @Override
    public void delete(Term term) {
        termAliasMapper.delete(Wrappers.lambdaQuery(TermAliasPO.class)
                .eq(TermAliasPO::getTermId, term.getId()));
        termMapper.deleteById(term.getId());
    }

    @Override
    public TermPage findPage(TermQuery query) {
        int offset = (query.getPageIndex() - 1) * query.getPageSize();
        List<TermPO> pos = termMapper.selectTermPage(query.getKeyword(), query.getStatus(),
                query.getOnlyCertified(), offset, query.getPageSize());
        long total = termMapper.countTermPage(query.getKeyword(), query.getStatus(),
                query.getOnlyCertified());
        List<Term> terms = pos.stream().map(this::assemble).collect(Collectors.toList());
        return TermPage.builder()
                .list(terms)
                .totalCount(total)
                .pageIndex(query.getPageIndex())
                .pageSize(query.getPageSize())
                .build();
    }

    /**
     * PO → 领域模型并装配别名。
     */
    private Term assemble(TermPO po) {
        Term term = termConvertor.toDomain(po);
        List<TermAliasPO> aliasPos = termAliasMapper.selectList(Wrappers.lambdaQuery(TermAliasPO.class)
                .eq(TermAliasPO::getTermId, po.getId()));
        term.setAliases(termConvertor.toAliasList(aliasPos));
        return term;
    }

    private void insertAliases(Long termId, List<String> aliases) {
        for (TermAliasPO aliasPo : termConvertor.toAliasPOList(termId, aliases)) {
            termAliasMapper.insert(aliasPo);
        }
    }

    @Override
    public List<Term> listAll() {
        List<TermPO> pos = termMapper.selectList(Wrappers.emptyWrapper());
        if (pos == null || pos.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return pos.stream().map(this::assemble).collect(Collectors.toList());
    }

    /**
     * 重建别名：删除旧别名 + 插入新别名（MV 规模每术语 1~5 条；批量优化 P1）。
     */
    private void replaceAliases(Long termId, List<String> aliases) {
        termAliasMapper.delete(Wrappers.lambdaQuery(TermAliasPO.class)
                .eq(TermAliasPO::getTermId, termId));
        insertAliases(termId, aliases);
    }
}
