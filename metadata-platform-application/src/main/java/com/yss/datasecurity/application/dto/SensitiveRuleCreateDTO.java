package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveRuleCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "特征名称不能为空")
    @Size(max = 128, message = "特征名称长度不能超过128字符")
    private String ruleName;

    private String ruleType; // BUILTIN / CUSTOM

    @Size(max = 1000, message = "特征描述长度不能超过1000字符")
    private String description;

    @Min(value = 1, message = "优先级最小为1")
    @Max(value = 100, message = "优先级最大为100")
    private Integer priority;

    private String categoryScopeMode; // ALL / TREE_NODE / SPECIFIC

    private List<Long> categoryScopeIds;

    private String scanScopeType; // COMPUTE_ENGINE / DATASOURCE

    private Object scanScopeConfig;
    private Object featureConfig;
}
