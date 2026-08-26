package com.yss.metadata.client.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集变更概览视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "采集变更概览视图对象")
public class MetadataDiffSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "实例 ID")
    private String instanceId;

    @ApiModelProperty(value = "数据源名称")
    private String datasourceName;

    @ApiModelProperty(value = "采集范围说明")
    private String collectScope;

    @ApiModelProperty(value = "采集策略说明")
    private String collectStrategy;

    @ApiModelProperty(value = "本次采集执行时间")
    private LocalDateTime executionTime;

    @ApiModelProperty(value = "本次采集对象总数")
    private Integer totalObjects;
    private Integer totalTables;
    private Integer totalViews;
    private Integer totalColumns;

    @ApiModelProperty(value = "新增对象统计")
    private Integer addedObjects;
    private Integer addedTables;
    private Integer addedViews;
    private Integer addedColumns;

    @ApiModelProperty(value = "更新对象统计")
    private Integer updatedObjects;
    private Integer updatedTables;
    private Integer updatedViews;
    private Integer updatedColumns;

    @ApiModelProperty(value = "删除对象统计")
    private Integer deletedObjects;
    private Integer deletedTables;
    private Integer deletedViews;
    private Integer deletedColumns;

    @ApiModelProperty(value = "表明细列表")
    private List<TableDiffVO> tableDetails;

    @ApiModelProperty(value = "视图明细列表")
    private List<ViewDiffVO> viewDetails;

    @ApiModelProperty(value = "字段明细列表")
    private List<ColumnDiffVO> columnDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableDiffVO implements Serializable {
        private String tableName;
        private String diffType;
        private Integer columnCount;
        private Long rowCount;
        private String changeDescription;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewDiffVO implements Serializable {
        private String viewName;
        private String diffType;
        private String definitionSql;
        private String changeDescription;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDiffVO implements Serializable {
        private String tableName;
        private String columnName;
        private String dataType;
        private String diffType;
        private String changeDescription;
        private LocalDateTime updatedAt;
    }
}
