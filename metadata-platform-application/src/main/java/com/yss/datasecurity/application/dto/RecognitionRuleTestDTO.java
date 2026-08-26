package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleTestDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private Long ruleId; // 可选已保存的规则ID
    private RecognitionRuleCreateDTO ruleDraft; // 或当前正在编辑的草案

    private String testScopeType; // PROJECT / DATASOURCE / TABLE

    @NotEmpty(message = "测试目标对象不能为空")
    @Size(max = 10, message = "测试范围最多选择10个项目或10张表")
    private List<String> targetIdentifiers; // 项目名/数据源ID/表全名
}
