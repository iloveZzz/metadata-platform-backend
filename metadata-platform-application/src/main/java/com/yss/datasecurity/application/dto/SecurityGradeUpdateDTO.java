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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityGradeUpdateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分级名称不能为空")
    @Size(max = 128, message = "分级名称长度不能超过128字符")
    @javax.validation.constraints.Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "分级名称仅支持中文、字母、数字或下划线")
    private String gradeName;

    @Size(max = 64, message = "分级缩写长度不能超过64字符")
    private String gradeCode;

    @Min(value = 1, message = "敏感程度权重最小为1")
    @Max(value = 100, message = "敏感程度权重最大为100")
    private Integer sensitivityScore;

    private String colorTag;

    @Size(max = 2048, message = "分级描述长度不能超过2048字符")
    private String description;
}
