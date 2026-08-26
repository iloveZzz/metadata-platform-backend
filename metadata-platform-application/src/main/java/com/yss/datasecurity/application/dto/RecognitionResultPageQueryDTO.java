package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("数据识别结果分页查询条件")
public class RecognitionResultPageQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("分类目录树节点ID")
    private Long treeNodeId;

    @ApiModelProperty("关键字搜索(表名/字段名/分类名)")
    private String keyword;

    @ApiModelProperty("数据分类ID")
    private Long categoryId;

    @ApiModelProperty("数据分类ID列表(用于多分类或父目录递归过滤)")
    private List<Long> categoryIds;

    @ApiModelProperty("数据安全分级ID")
    private Long securityGradeId;

    @ApiModelProperty("是否已锁定")
    private Boolean isLocked;

    @ApiModelProperty("数据源ID")
    private String datasourceId;

    @ApiModelProperty("脱敏生效状态(ENABLED/DISABLED)")
    private String maskingStatus;

    @ApiModelProperty("识别方式(AUTO/MANUAL/LINEAGE)")
    private String recognitionMethod;

    @ApiModelProperty("资产来源类型(DATAPHIN/DATASOURCE)")
    private String assetSourceType;

    @ApiModelProperty("是否存在更优推荐结果")
    private Boolean hasBetterRecommendation;
}
