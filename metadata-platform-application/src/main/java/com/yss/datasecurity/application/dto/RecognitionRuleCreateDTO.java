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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "识别规则名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,12}$", message = "识别规则名称只能包含中文、字母、数字、下划线，且不能超过12个字符")
    private String ruleName;

    @Size(max = 128, message = "识别规则说明不能超过128个字符")
    private String description;

    private String categoryScopeMode; // ALL / TREE_NODE / SPECIFIC
    private Object categoryScopeConfig;

    private String scanSourceType; // COMPUTE_ENGINE / DATASOURCE
    private Object computeScopeConfig;
    private Object datasourceScopeConfig;

    @Min(value = 1, message = "优先级最小为1")
    @Max(value = 100, message = "优先级最大为100")
    private Integer priority;

    private String owner;
    private Boolean lineageInheritanceEnabled;
}
