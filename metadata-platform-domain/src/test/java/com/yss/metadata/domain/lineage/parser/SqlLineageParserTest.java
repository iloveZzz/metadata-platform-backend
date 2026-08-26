package com.yss.metadata.domain.lineage.parser;

import com.yss.metadata.domain.lineage.parser.model.ColumnLineage;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
import com.yss.metadata.domain.lineage.parser.model.TableLineage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 血缘解析器行为测试（WU-03-02，TDD 红→绿；FR-010 抽样准确率 ≥80%）。
 *
 * <p>覆盖：INSERT...SELECT（表级+列级）、SELECT/建表样本、CTE、OceanBase 方言
 * （INSERT OVERWRITE）、GaussDB 方言 seam-deferred（明确不支持提示，不伪装解析）。
 * 抽样准确率证据见 {@link #samplingAccuracyAboveThreshold()}。</p>
 */
class SqlLineageParserTest {

    private final SqlLineageParser parser = new DefaultSqlLineageParser();

    // ---------- 表级血缘 ----------

    @Test
    @DisplayName("INSERT...SELECT 生成 表级血缘 src→tgt")
    void insertSelectTableLineage() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di SELECT * FROM ods_order WHERE status = 1");

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsExactly("dwd_order_di");
        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactly("ods_order");
    }

    @Test
    @DisplayName("多源 INSERT...SELECT：JOIN 各源表均生成边")
    void insertSelectMultiSource() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO ads_pay_di SELECT o.order_id, u.user_name FROM ods_order o "
                        + "JOIN ods_user u ON o.user_id = u.user_id");

        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactlyInAnyOrder("ods_order", "ods_user");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsOnly("ads_pay_di");
    }

    @Test
    @DisplayName("INSERT 显式列清单 + SELECT 表达式：位置映射列级血缘 src.col→tgt.col")
    void insertSelectColumnLineage() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di (order_id, amount, status) "
                        + "SELECT id, total_amount, order_status FROM ods_order");

        assertThat(result.isSupported()).isTrue();
        List<ColumnLineage> columns = result.getColumnLineage();
        assertThat(columns).hasSize(3);
        assertThat(columns).extracting(ColumnLineage::getToTable).containsOnly("dwd_order_di");
        assertThat(columns).extracting(ColumnLineage::getFromTable).containsOnly("ods_order");
        assertThat(columns).extracting(ColumnLineage::getToColumn)
                .containsExactly("order_id", "amount", "status");
        assertThat(columns).extracting(ColumnLineage::getFromColumn)
                .containsExactly("id", "total_amount", "order_status");
    }

    @Test
    @DisplayName("SELECT 表达式带表限定与别名：解析出基列")
    void insertSelectQualifiedColumn() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di (order_id) SELECT o.id AS oid FROM ods_order o");

        assertThat(result.getColumnLineage())
                .extracting(ColumnLineage::getFromColumn).containsExactly("id");
        assertThat(result.getColumnLineage())
                .extracting(ColumnLineage::getToColumn).containsExactly("order_id");
    }

    @Test
    @DisplayName("CREATE TABLE AS SELECT：生成 src→tgt")
    void createTableAsSelect() {
        SqlLineageResult result = parser.parse(
                "CREATE TABLE dim_date AS SELECT d.id, d.name FROM ods_date d");

        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactly("ods_date");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsExactly("dim_date");
    }

    @Test
    @DisplayName("CREATE VIEW AS SELECT：视图→源表")
    void createViewAsSelect() {
        SqlLineageResult result = parser.parse(
                "CREATE VIEW v_order_amt AS SELECT o.amount FROM ods_order o");

        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactly("ods_order");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsExactly("v_order_amt");
    }

    @Test
    @DisplayName("CTE：CTE 体源表 + 外部源表均生成边，CTE 别名不入边")
    void cteSourcesExtracted() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di "
                        + "WITH filtered AS (SELECT * FROM ods_order WHERE status = 1) "
                        + "SELECT f.order_id FROM filtered f JOIN ods_user u ON f.user_id = u.user_id");

        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactlyInAnyOrder("ods_order", "ods_user");
        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .doesNotContain("filtered");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsOnly("dwd_order_di");
    }

    @Test
    @DisplayName("子查询源表被召回：FROM (SELECT ... FROM x) 解析出 x")
    void subquerySourceExtracted() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_top_di SELECT * FROM (SELECT * FROM ods_flow WHERE level > 0) t");

        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactly("ods_flow");
    }

    // ---------- OceanBase 兼容 ----------

    @Test
    @DisplayName("OceanBase INSERT OVERWRITE TABLE：正常解析")
    void oceanBaseInsertOverwrite() {
        SqlLineageResult result = parser.parse(
                "INSERT OVERWRITE TABLE dwd_customer_di SELECT * FROM ods_customer");

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactly("ods_customer");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsExactly("dwd_customer_di");
    }

    // ---------- 无目标 / VALUES ----------

    @Test
    @DisplayName("无目标 SELECT：supported 且无表级血缘（无目标表无从建边）")
    void bareSelectNoTableLineage() {
        SqlLineageResult result = parser.parse("SELECT order_id, amount FROM ods_order WHERE amount > 0");

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).isEmpty();
    }

    @Test
    @DisplayName("INSERT...VALUES：无源表，无表级血缘")
    void insertValuesNoSource() {
        SqlLineageResult result = parser.parse("INSERT INTO dim_status (id, name) VALUES (1, 'active')");

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).isEmpty();
    }

    // ---------- GaussDB 方言 seam-deferred ----------

    @Test
    @DisplayName("GaussDB 方言 hint：明确返回不支持提示，不伪装解析")
    void gaussDbDialectHintUnsupported() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di SELECT * FROM ods_order", "gaussdb");

        assertThat(result.isSupported()).isFalse();
        assertThat(result.getUnsupportedReason()).isNotBlank();
        assertThat(result.getTableLineage()).isEmpty();
    }

    @Test
    @DisplayName("识别 GaussDB/PostgreSQL 语法（RETURNING）：返回不支持提示")
    void pgReturningDetected() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di SELECT * FROM ods_order RETURNING id");

        assertThat(result.isSupported()).isFalse();
        assertThat(result.getUnsupportedReason()).contains("GaussDB");
    }

    @Test
    @DisplayName("识别 PG 强制转换 ::：返回不支持提示")
    void pgCastOperatorDetected() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dwd_order_di SELECT id::bigint FROM ods_order");

        assertThat(result.isSupported()).isFalse();
        assertThat(result.getUnsupportedReason()).contains("GaussDB");
    }

    @Test
    @DisplayName("识别 PG ON CONFLICT：返回不支持提示")
    void pgOnConflictDetected() {
        SqlLineageResult result = parser.parse(
                "INSERT INTO dim_date (id) SELECT id FROM ods_date ON CONFLICT (id) DO NOTHING");

        assertThat(result.isSupported()).isFalse();
        assertThat(result.getUnsupportedReason()).contains("GaussDB");
    }

    // ---------- 抽样准确率证据（FR-010 ≥80%） ----------

    @Test
    @DisplayName("抽样准确率证据：13 条方言样本正确解析 ≥ 80%")
    void samplingAccuracyAboveThreshold() {
        String[][] samples = {
                // 样本 | 期望 from | 期望 to
                {"INSERT INTO t1 SELECT * FROM s1", "s1", "t1"},
                {"INSERT INTO t2 (a, b) SELECT x, y FROM s2", "s2", "t2"},
                {"INSERT INTO t3 SELECT a.*, b.* FROM s3 a JOIN s4 b ON a.id = b.id", "s3", "t3"},
                {"INSERT INTO t3 SELECT a.*, b.* FROM s3 a JOIN s4 b ON a.id = b.id", "s4", "t3"},
                {"CREATE TABLE t4 AS SELECT * FROM s5", "s5", "t4"},
                {"CREATE VIEW v1 AS SELECT id FROM s6", "s6", "v1"},
                {"INSERT INTO t5 WITH c AS (SELECT * FROM s7) SELECT * FROM c", "s7", "t5"},
                {"INSERT OVERWRITE TABLE t6 SELECT * FROM s8", "s8", "t6"},
                {"INSERT INTO t7 SELECT * FROM (SELECT id FROM s9) x", "s9", "t7"},
                {"INSERT INTO t8 SELECT s1.id, s2.name FROM s10 s1, s11 s2 WHERE s1.id = s2.id", "s10", "t8"},
                {"INSERT INTO t8 SELECT s1.id, s2.name FROM s10 s1, s11 s2 WHERE s1.id = s2.id", "s11", "t8"},
                {"INSERT INTO t9 SELECT * FROM db1.s12", "db1.s12", "t9"},
                {"INSERT INTO t10 (id) SELECT id FROM s13 UNION ALL SELECT id FROM s14", "s13", "t10"},
        };

        int matched = 0;
        for (String[] sample : samples) {
            SqlLineageResult result = parser.parse(sample[0]);
            boolean hit = result.isSupported()
                    && result.getTableLineage().stream().anyMatch(e ->
                    e.getFromTable().equals(sample[1]) && e.getToTable().equals(sample[2]));
            if (hit) {
                matched++;
            }
        }
        double accuracy = matched * 100.0 / samples.length;
        assertThat(accuracy).as("抽样准确率 %s%%（%d/%d）应 ≥ 80%%", accuracy, matched, samples.length)
                .isGreaterThanOrEqualTo(80.0);
    }
}
