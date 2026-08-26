package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 采集实例重跑命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "采集实例重跑命令")
public class CollectorInstanceRerunCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "操作人工号/姓名")
    private String operator;
}
