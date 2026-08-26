package com.yss.datamiddle.semantic.term.gateway;

import com.yss.datamiddle.semantic.term.model.Term;

import java.util.Optional;

/**
 * 术语持久化网关（领域接口，Infrastructure 实现）。
 *
 * <p>聚合事务边界：term + term_alias 同事务（Application 用例边界）+ 写审计同事务。</p>
 */
public interface TermGateway {

    /**
     * 按 id 查询（含别名）。
     */
    Optional<Term> findById(Long id);

    /**
     * 按名称唯一查询（含别名）。
     */
    Optional<Term> findByName(String name);

    /**
     * 名称唯一性检查（排除指定 id，用于编辑场景）。
     */
    boolean existsByName(String name, Long excludeId);

    /**
     * 新建（term + aliases 插入）。
     */
    void save(Term term);

    /**
     * 乐观锁条件更新（WHERE id = ? AND version = expectedVersion）。
     *
     * <p>term 更新成功后重建别名（删除旧别名 + 插入新别名）。匹配 0 行返回 false，
     * 由 Application 重新加载最新对象并抛 {@code VersionConflictException}（409 VERSION_CONFLICT + 最新对象）。</p>
     *
     * @return 条件更新是否命中（true=成功 / false=版本过期或已被删除）
     */
    boolean updateWithVersion(Term term, int expectedVersion);

    /**
     * 物理删除（term + aliases）。
     */
    void delete(Term term);

    /**
     * 分页查询（keyword / status / onlyCertified 筛选，0 条以空分页表达）。
     */
    TermPage findPage(TermQuery query);

    /**
     * 查询所有术语（用于全量语料池或同义词推荐计算）。
     */
    java.util.List<Term> listAll();
}
