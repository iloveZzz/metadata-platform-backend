package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量实例操作命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "批量实例操作命令")
public class BatchInstanceCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "实例 ID 列表不可为空")
    @ApiModelProperty(value = "实例 ID 列表", required = true)
    private List<String> instanceIds;

    @ApiModelProperty(value = "操作人")
    private String operator;

    @ApiModelProperty(value = "原因说明 (终止时生效)")
    private String reason;
}
