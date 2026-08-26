package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskQueryEvaluationRequestDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "数据源ID不能为空")
    private String datasourceId;

    @NotBlank(message = "表名不能为空")
    private String tableName;

    @NotEmpty(message = "待脱敏数据行列表不能为空")
    private List<Map<String, Object>> rawRows;
}
