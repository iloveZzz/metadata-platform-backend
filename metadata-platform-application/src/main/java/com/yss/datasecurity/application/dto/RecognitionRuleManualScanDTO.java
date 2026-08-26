package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleManualScanDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "扫描范围类型不能为空")
    private String scanScopeType; // ALL_DB / SPECIFIC_PROJECT / SPECIFIC_DATASOURCE / SPECIFIC_TABLES

    @Size(max = 10, message = "指定表扫描最多不超过10张数据表")
    private List<String> targetIdentifiers; // 项目列表 / 数据源ID列表 / 数据表列表

    @NotBlank(message = "规则执行范围不能为空")
    private String ruleScope; // ENABLED_ONLY / ALL_RULES
}
