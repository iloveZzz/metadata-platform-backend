package com.yss.metadata.domain.lineage.parser.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQL 血缘解析结果（表级 + 列级；方言支持状态）。
 *
 * <p>supported=false 表示方言不支持（GaussDB seam-deferred），表/列血缘为空，
 * unsupportedReason 给出明确提示（不伪装解析）。表名/列名为 SQL 文本中的
 * 原始标识符（资产映射为 SQL 来源 seam，属切片 05）。</p>
 */
@Getter
public class SqlLineageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean supported;

    private final String unsupportedReason;

    private final List<TableLineage> tableLineage;

    private final List<ColumnLineage> columnLineage;

    private SqlLineageResult(boolean supported, String unsupportedReason,
                             List<TableLineage> tableLineage, List<ColumnLineage> columnLineage) {
        this.supported = supported;
        this.unsupportedReason = unsupportedReason;
        this.tableLineage = tableLineage == null ? Collections.emptyList() : tableLineage;
        this.columnLineage = columnLineage == null ? Collections.emptyList() : columnLineage;
    }

    public static SqlLineageResult supported(List<TableLineage> tableLineage,
                                             List<ColumnLineage> columnLineage) {
        return new SqlLineageResult(true, null, tableLineage, columnLineage);
    }

    public static SqlLineageResult unsupported(String reason) {
        return new SqlLineageResult(false, reason, new ArrayList<>(), new ArrayList<>());
    }
}
