package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import io.swagger.annotations.ApiModel;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * AI 智能找数请求命令
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "AI 智能找数请求命令")
public class AskMetadataCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "查询意图不能为空")
    @ApiModelProperty(value = "自然语言提问内容", required = true, example = "帮我找公募基金高净值客户交易流水表")
    private String query;

    @ApiModelProperty(value = "可选限定数据域", example = "trade")
    private String domain;

    @ApiModelProperty(value = "返回卡片数量限制（默认 5）", example = "5")
    private Integer limit;
}
