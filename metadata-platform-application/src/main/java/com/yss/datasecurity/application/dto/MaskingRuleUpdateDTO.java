package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingRuleUpdateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private String ruleName;

    @NotNull(message = "目标数据分类ID不能为空")
    private Long categoryId;

    private String description;

    @NotBlank(message = "脱敏算法类型不能为空")
    private String algorithmType;

    private String subAlgorithm;

    private Map<String, Object> algorithmParams;

    private String applyScene;
    private String maskMethod;
    private String plateScope;
    private String projectScope;
    private String scopeType;
    private Map<String, Object> scopeTarget;
    private Long keyId;
    private String owner;
    private String status;
}
