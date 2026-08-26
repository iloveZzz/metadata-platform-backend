package com.yss.metadata.domain.governance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分类规则（数据架构 ClassRule：id/name/type/pattern/enabled）。
 *
 * <p>内置规则（手机号/身份证/银行卡/邮箱）+ 自定义正则/列名/字典（FR-016）；
 * 启停有审计（应用层写 audit_log）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 规则名 */
    private String name;

    /** 规则类型（builtin/regex/column/dictionary） */
    private ClassRuleType type;

    /** 匹配模式（正则表达式 / 列名关键字 / 字典引用） */
    private String pattern;

    /** 是否启用 */
    private Boolean enabled;
}
