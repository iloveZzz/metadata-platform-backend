package com.yss.metadata.domain.lineage.parser;

import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;

/**
 * SQL 血缘解析器端口（Domain seam；WU-03-02）。
 *
 * <p>解析 SQL 文本 → 表级 + 列级血缘；MySQL/OceanBase 兼容先行；
 * GaussDB 方言 seam-deferred：识别到 GaussDB/PostgreSQL 语法时明确返回
 * 不支持提示（不伪装解析）。SQL 来源（采集 SQL 收集 / OpenLineage 事件）
 * 为切片 05 seam。</p>
 */
public interface SqlLineageParser {

    /**
     * 解析 SQL（方言自动探测：GaussDB/PostgreSQL 语法返回不支持）。
     */
    SqlLineageResult parse(String sql);

    /**
     * 解析 SQL（带方言提示，如连接器 dialect=gaussdb）。
     *
     * @param dialectHint 方言提示（gaussdb 直接返回不支持；null/其他走自动探测）
     */
    SqlLineageResult parse(String sql, String dialectHint);
}
