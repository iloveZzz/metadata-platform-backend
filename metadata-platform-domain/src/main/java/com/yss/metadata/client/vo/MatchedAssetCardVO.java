package com.yss.metadata.client.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推荐资产卡片 VO
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "推荐资产卡片 VO")
public class MatchedAssetCardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("资产ID")
    private String assetId;

    @ApiModelProperty("资产名称")
    private String assetName;

    @ApiModelProperty("资产描述")
    private String title;

    @ApiModelProperty("所属数据域")
    private String domain;

    @ApiModelProperty("置信度提示")
    private String confidenceText;

    @ApiModelProperty("匹配解释")
    private String matchReason;

    @ApiModelProperty("健康分")
    private Integer healthScore;

    @ApiModelProperty("质量等级")
    private String qualityBand;

    @ApiModelProperty("数据存疑状态")
    private String taintStatus;
}
