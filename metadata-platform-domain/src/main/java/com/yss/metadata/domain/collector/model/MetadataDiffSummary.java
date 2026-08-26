package com.yss.metadata.domain.collector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集变更概览值对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataDiffSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实例 ID */
    private String instanceId;

    /** 数据源名称 */
    private String datasourceName;

    /** 采集范围说明 */
    private String collectScope;

    /** 采集策略说明 */
    private String collectStrategy;

    /** 本次采集执行时间 */
    private LocalDateTime executionTime;

    /** 对象统计 */
    private Integer totalObjects;
    private Integer totalTables;
    private Integer totalViews;
    private Integer totalColumns;

    /** 新增统计 */
    private Integer addedObjects;
    private Integer addedTables;
    private Integer addedViews;
    private Integer addedColumns;

    /** 更新统计 */
    private Integer updatedObjects;
    private Integer updatedTables;
    private Integer updatedViews;
    private Integer updatedColumns;

    /** 删除统计 */
    private Integer deletedObjects;
    private Integer deletedTables;
    private Integer deletedViews;
    private Integer deletedColumns;

    /** 表明细列表 */
    @Builder.Default
    private List<TableDiffItem> tableDetails = new ArrayList<>();

    /** 视图明细列表 */
    @Builder.Default
    private List<ViewDiffItem> viewDetails = new ArrayList<>();

    /** 字段明细列表 */
    @Builder.Default
    private List<ColumnDiffItem> columnDetails = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableDiffItem implements Serializable {
        private String tableName;
        private String diffType; // ADDED, UPDATED, DELETED
        private Integer columnCount;
        private Long rowCount;
        private String changeDescription;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewDiffItem implements Serializable {
        private String viewName;
        private String diffType; // ADDED, UPDATED, DELETED
        private String definitionSql;
        private String changeDescription;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDiffItem implements Serializable {
        private String tableName;
        private String columnName;
        private String dataType;
        private String diffType; // ADDED, UPDATED, DELETED
        private String changeDescription;
        private LocalDateTime updatedAt;
    }
}
