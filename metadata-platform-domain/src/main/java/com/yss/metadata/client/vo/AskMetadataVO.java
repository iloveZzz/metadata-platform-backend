package com.yss.metadata.client.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * AI 智能找数响应 VO
 *
 * @author ai
 * @since 2026-08-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI 智能找数响应 VO")
public class AskMetadataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("原始查询")
    private String query;

    @ApiModelProperty("AI 解答回复")
    private String reply;

    @ApiModelProperty("意图解析摘要")
    private String queryIntent;

    @ApiModelProperty("匹配资产卡片列表")
    private List<MatchedAssetCardVO> matchedAssets;

    @ApiModelProperty("是否降级为本地检索")
    private boolean fallback;
}
