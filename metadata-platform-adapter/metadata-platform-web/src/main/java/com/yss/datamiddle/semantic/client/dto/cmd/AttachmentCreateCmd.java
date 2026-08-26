package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AttachmentCreateCmd extends CommandDTO {
    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @NotBlank(message = "挂接层级不能为空")
    private String level;

    private String columnName;

    @NotBlank(message = "语义对象类型不能为空")
    private String semanticType;

    @NotNull(message = "语义对象ID不能为空")
    private Long semanticId;
}
