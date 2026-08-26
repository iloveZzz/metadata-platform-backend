package com.yss.metadata.domain.governance.gateway;

import com.yss.metadata.domain.governance.model.ClassRule;

import java.util.List;
import java.util.Optional;

/**
 * 分类规则仓储端口（治理域；Domain 定义，Infrastructure 实现）。
 *
 * <p>class_rule 表：规则列表、启用的内置/自定义规则（识别引擎输入）与保存（含启停）。</p>
 */
public interface ClassRuleGateway {

    /**
     * 查询全部规则（按 id 序；无创建时间列，排序稳定即可）。
     */
    List<ClassRule> findAll();

    /**
     * 查询全部启用的规则（识别引擎输入）。
     */
    List<ClassRule> findEnabled();

    /**
     * 按 id 查询规则。
     */
    Optional<ClassRule> findById(String id);

    /**
     * 保存规则（新增或更新，含启停）。
     */
    ClassRule save(ClassRule rule);
}
