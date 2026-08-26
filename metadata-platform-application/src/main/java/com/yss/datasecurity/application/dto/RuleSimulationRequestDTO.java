package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleSimulationRequestDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "数据源标识不能为空")
    private String datasourceId;

    @NotEmpty(message = "模拟测试表列表不能为空")
    @Size(max = 10, message = "单次模拟测试不能超过10张表")
    private List<String> tableNames;

    @NotNull(message = "规则草稿配置不能为空")
    @Valid
    private SensitiveRuleCreateDTO ruleDraftConfig;
}
