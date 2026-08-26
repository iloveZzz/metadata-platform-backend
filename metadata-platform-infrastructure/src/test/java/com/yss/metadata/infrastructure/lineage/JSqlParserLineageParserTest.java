package com.yss.metadata.infrastructure.lineage;

import com.yss.metadata.domain.lineage.parser.model.ColumnLineage;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
import com.yss.metadata.domain.lineage.parser.model.TableLineage;
import com.yss.metadata.infrastructure.lineage.parser.JSqlParserLineageParserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSqlParserLineageParserImpl 单元测试。
 * 覆盖单表、多表 JOIN、聚合函数、计算表达式、CTE、子查询、方言提示与降级容错。
 */
class JSqlParserLineageParserTest {

    private JSqlParserLineageParserImpl parser;

    @BeforeEach
    void setUp() {
        parser = new JSqlParserLineageParserImpl();
    }

    @Test
    @DisplayName("测试1: 单表 CREATE VIEW 直接映射与别名解析")
    void testSingleTableCreateViewDirect() {
        String sql = "CREATE VIEW v_user_summary AS SELECT id, user_name AS name, email FROM ods_users";
        SqlLineageResult result = parser.parse(sql);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).hasSize(1);
        TableLineage tableLineage = result.getTableLineage().get(0);
        assertThat(tableLineage.getFromTable()).isEqualTo("ods_users");
        assertThat(tableLineage.getToTable()).isEqualTo("v_user_summary");

        List<ColumnLineage> columns = result.getColumnLineage();
        assertThat(columns).hasSize(3);

        ColumnLineage c1 = columns.get(0);
        assertThat(c1.getFromTable()).isEqualTo("ods_users");
        assertThat(c1.getFromColumn()).isEqualTo("id");
        assertThat(c1.getToColumn()).isEqualTo("id");
        assertThat(c1.getExprType()).isEqualTo("DIRECT");

        ColumnLineage c2 = columns.get(1);
        assertThat(c2.getFromTable()).isEqualTo("ods_users");
        assertThat(c2.getFromColumn()).isEqualTo("user_name");
        assertThat(c2.getToColumn()).isEqualTo("name");
        assertThat(c2.getExprType()).isEqualTo("DIRECT");
    }

    @Test
    @DisplayName("测试2: 多表 JOIN 与 AGGREGATE 聚合运算表达式抽取")
    void testMultiTableJoinWithAggregate() {
        String sql = "CREATE VIEW v_user_order_stat AS " +
                "SELECT u.region, sum(o.amount) AS total_amount, count(o.id) AS order_cnt " +
                "FROM orders o JOIN users u ON o.user_id = u.id " +
                "GROUP BY u.region";

        SqlLineageResult result = parser.parse(sql);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).extracting(TableLineage::getFromTable)
                .containsExactlyInAnyOrder("orders", "users");
        assertThat(result.getTableLineage()).extracting(TableLineage::getToTable)
                .containsOnly("v_user_order_stat");

        List<ColumnLineage> columns = result.getColumnLineage();
        assertThat(columns).hasSize(3);

        ColumnLineage regionCol = columns.stream()
                .filter(c -> "region".equals(c.getToColumn()))
                .findFirst().orElseThrow(AssertionError::new);
        assertThat(regionCol.getFromTable()).isEqualTo("users");
        assertThat(regionCol.getFromColumn()).isEqualTo("region");
        assertThat(regionCol.getExprType()).isEqualTo("DIRECT");

        ColumnLineage totalAmountCol = columns.stream()
                .filter(c -> "total_amount".equals(c.getToColumn()))
                .findFirst().orElseThrow(AssertionError::new);
        assertThat(totalAmountCol.getFromTable()).isEqualTo("orders");
        assertThat(totalAmountCol.getFromColumn()).isEqualTo("amount");
        assertThat(totalAmountCol.getExprType()).isEqualTo("AGGREGATE");
        assertThat(totalAmountCol.getTransformExpr()).contains("sum(o.amount)");
    }

    @Test
    @DisplayName("测试3: INSERT INTO 指定目标列映射")
    void testInsertIntoSelect() {
        String sql = "INSERT INTO dwd_order_di (order_id, user_id, pay_amt, pay_status) " +
                "SELECT o.id, o.uid, o.amount * 0.9, upper(o.status) FROM ods_order o";

        SqlLineageResult result = parser.parse(sql);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).hasSize(1);
        assertThat(result.getTableLineage().get(0).getToTable()).isEqualTo("dwd_order_di");

        List<ColumnLineage> columns = result.getColumnLineage();
        assertThat(columns).hasSize(4);

        assertThat(columns.get(0).getToColumn()).isEqualTo("order_id");
        assertThat(columns.get(0).getFromColumn()).isEqualTo("id");
        assertThat(columns.get(0).getExprType()).isEqualTo("DIRECT");

        assertThat(columns.get(2).getToColumn()).isEqualTo("pay_amt");
        assertThat(columns.get(2).getFromColumn()).isEqualTo("amount");
        assertThat(columns.get(2).getExprType()).isEqualTo("COMPUTED");
        assertThat(columns.get(2).getTransformExpr()).contains("o.amount * 0.9");
    }

    @Test
    @DisplayName("测试4: CTE 公共表表达式解析与别名排除")
    void testCteSelectLineage() {
        String sql = "CREATE VIEW v_high_value_orders AS " +
                "WITH filtered_orders AS (SELECT id, amount, user_id FROM ods_orders WHERE amount > 100) " +
                "SELECT id, amount, user_id FROM filtered_orders";

        SqlLineageResult result = parser.parse(sql);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).isNotEmpty();
        assertThat(result.getTableLineage().get(0).getFromTable()).isEqualTo("ods_orders");
        assertThat(result.getTableLineage().get(0).getToTable()).isEqualTo("v_high_value_orders");
        assertThat(result.getColumnLineage()).isNotEmpty();
    }

    @Test
    @DisplayName("测试5: SELECT * 全字段通配映射")
    void testWildcardSelect() {
        String sql = "CREATE TABLE dwd_trade AS SELECT * FROM ods_trade";
        SqlLineageResult result = parser.parse(sql);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.getTableLineage()).hasSize(1);
        assertThat(result.getColumnLineage()).hasSize(1);
        assertThat(result.getColumnLineage().get(0).getFromColumn()).isEqualTo("*");
        assertThat(result.getColumnLineage().get(0).getToColumn()).isEqualTo("*");
        assertThat(result.getColumnLineage().get(0).getExprType()).isEqualTo("DIRECT");
    }

    @Test
    @DisplayName("测试6: 空 SQL 与 GaussDB 方言拦截")
    void testDialectAndEmptyHandling() {
        SqlLineageResult emptyResult = parser.parse("");
        assertThat(emptyResult.isSupported()).isFalse();

        SqlLineageResult gaussResult = parser.parse("SELECT * FROM t", "gaussdb");
        assertThat(gaussResult.isSupported()).isFalse();
        assertThat(gaussResult.getUnsupportedReason()).contains("GaussDB");
    }
}
