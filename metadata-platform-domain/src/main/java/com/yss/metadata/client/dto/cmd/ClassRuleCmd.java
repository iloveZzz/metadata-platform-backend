package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 新增/修正分类规则命令（冻结 OpenAPI POST /api/classifications configure）。
 *
 * <p>冻结 spec 未声明 requestBody（沿切片 02 PUT /connectors/{id} 先例，
 * 前端经 options.data 透传 body；后端以本 Cmd 为契约）。</p>
 */
@Getter
@Setter
public class ClassRuleCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 规则名 */
    @NotBlank(message = "规则名不能为空")
    private String name;

    /** 规则类型（builtin/regex/column/dictionary） */
    @NotNull(message = "规则类型不能为空")
    private ClassRuleType type;

    /** 匹配模式（正则表达式 / 列名关键字 / 字典引用） */
    @NotBlank(message = "匹配模式不能为空")
    private String pattern;

    /** 是否启用（默认 true） */
    private Boolean enabled = Boolean.TRUE;
}
