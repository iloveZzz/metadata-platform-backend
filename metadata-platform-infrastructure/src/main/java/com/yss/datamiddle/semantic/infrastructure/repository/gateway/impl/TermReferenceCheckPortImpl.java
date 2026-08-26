package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.term.gateway.TermReferenceCheckPort;
import org.springframework.stereotype.Repository;

/**
 * 删除引用检查实现（可测 seam，SL-SLICE-01）。
 *
 * <p>attachment / synonym_set 表分别由 SL-SLICE-04 / 03 建表（数据架构 §11 表归属互斥），
 * 本切片未建表，默认返回无引用；03 / 04 切片建表后替换实现为真实查询
 * （挂接 active 记录 / synonym_set.term_id 命中 → 409 REFERENCE_CONFLICT，SB-09）。
 * CT-07 引用冲突路径由测试替身验证。</p>
 */
@Repository
public class TermReferenceCheckPortImpl implements TermReferenceCheckPort {

    @Override
    public boolean isReferenced(Long termId) {
        // TODO-HUMAN-REVIEW: SL-SLICE-04（attachment 挂接）/ SL-SLICE-03（synonym_set 关联）
        // 建表后接入真实引用检查；当前表归属互斥下返回无引用
        return false;
    }
}
