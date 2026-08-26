package com.yss.metadata.domain.lineage.parser.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 表级血缘（SQL 文本表名级；from=源表，to=目标表）。
 */
@Getter
public class TableLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源表名（可含库前缀 db.table） */
    private final String fromTable;

    /** 目标表名（INSERT 目标 / CREATE TABLE|VIEW AS） */
    private final String toTable;

    public TableLineage(String fromTable, String toTable) {
        this.fromTable = fromTable;
        this.toTable = toTable;
    }
}
