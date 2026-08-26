package com.yss.datamiddle.semantic.infrastructure.repository.convertor;

import com.yss.datamiddle.semantic.infrastructure.repository.po.TermAliasPO;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermPO;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 术语 PO ↔ 领域模型 Convertor（MapStruct，Spring 组件模型）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface TermConvertor {

    /**
     * PO → 领域模型（aliases 由 Gateway 从 term_alias 表装配）。
     */
    @Mapping(target = "aliases", ignore = true)
    Term toDomain(TermPO po);

    /**
     * 领域模型 → PO。
     */
    TermPO toPO(Term term);

    /**
     * 状态字符串 → 枚举（draft / certified / deprecated）。
     */
    default TermStatus toStatus(String code) {
        return code == null ? null : TermStatus.fromCode(code);
    }

    /**
     * 状态枚举 → 字符串。
     */
    default String fromStatus(TermStatus status) {
        return status == null ? null : status.getCode();
    }

    /**
     * 构建别名 PO（值对象 → PO 行）。
     */
    default TermAliasPO toAliasPO(Long termId, String alias) {
        TermAliasPO po = new TermAliasPO();
        po.setTermId(termId);
        po.setAlias(alias);
        return po;
    }

    /**
     * 别名集合 → PO 行集合。
     */
    default List<TermAliasPO> toAliasPOList(Long termId, List<String> aliases) {
        if (aliases == null) {
            return Collections.emptyList();
        }
        return aliases.stream()
                .map(alias -> toAliasPO(termId, alias))
                .collect(Collectors.toList());
    }

    /**
     * 别名 PO 行集合 → 别名集合（读路径）。
     */
    default List<String> toAliasList(List<TermAliasPO> aliasPos) {
        if (aliasPos == null) {
            return Collections.emptyList();
        }
        return aliasPos.stream()
                .map(TermAliasPO::getAlias)
                .collect(Collectors.toList());
    }
}
