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
public class MaskingRuleCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotNull(message = "目标数据分类ID不能为空")
    private Long categoryId;

    private String description;

    @NotBlank(message = "脱敏算法类型不能为空")
    private String algorithmType; // MASK / HASH / CRYPTO / OTHER

    private String subAlgorithm;

    private Map<String, Object> algorithmParams;

    private String applyScene; // WRITE_DEV_TABLE / DATA_QUERY / ALL
    private String maskMethod; // UNDERLYING / DISPLAY
    private String plateScope;
    private String projectScope;
    private String scopeType; // GLOBAL / DATASOURCE / PROJECT
    private Map<String, Object> scopeTarget;
    private Long keyId;
    private String owner;
    private String status;
}
