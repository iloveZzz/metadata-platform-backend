package com.yss.metadata.domain.lineage.parser.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 列级血缘（SQL 文本标识符级；from 源表.源列 → to 目标表.目标列）。
 */
@Getter
public class ColumnLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源表名 */
    private final String fromTable;

    /** 源列名 */
    private final String fromColumn;

    /** 目标表名 */
    private final String toTable;

    /** 目标列名 */
    private final String toColumn;

    /** 字段转换表达式 */
    private final String transformExpr;

    /** 表达式类型：DIRECT(直通)/COMPUTED(计算)/AGGREGATE(聚合)/MANUAL(人工) */
    private final String exprType;

    public ColumnLineage(String fromTable, String fromColumn, String toTable, String toColumn) {
        this(fromTable, fromColumn, toTable, toColumn, fromColumn, "DIRECT");
    }

    public ColumnLineage(String fromTable, String fromColumn, String toTable, String toColumn,
                         String transformExpr, String exprType) {
        this.fromTable = fromTable;
        this.fromColumn = fromColumn;
        this.toTable = toTable;
        this.toColumn = toColumn;
        this.transformExpr = transformExpr != null ? transformExpr : fromColumn;
        this.exprType = exprType != null ? exprType : "DIRECT";
    }
}
