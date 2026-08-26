package com.yss.datamiddle.semantic.term.gateway;

/**
 * 删除引用检查端口（跨聚合协调，领域层定义）。
 *
 * <p>删除前检查术语是否已被挂接（attachment，SL-SLICE-04 表）或被同义词组关联
 * （synonym_set，SL-SLICE-03 表），命中返回 true 由 Application 抛 409 REFERENCE_CONFLICT。</p>
 *
 * <p>表归属互斥（数据架构 §11）：attachment / synonym_set 分别由 04 / 03 主导写且本切片未建表，
 * 因此本端口在 SL-SLICE-01 以可测 seam 落位（默认实现返回无引用），CT-07 引用冲突路径
 * 由测试替身验证；03/04 切片建表后替换实现。</p>
 */
public interface TermReferenceCheckPort {

    /**
     * @return true = 已被挂接或关联同义词组，禁止物理删除
     */
    boolean isReferenced(Long termId);
}
