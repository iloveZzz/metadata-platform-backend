package com.yss.datamiddle.aicontextlayer.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WU-01-02 SchemaConstraintTest（受控生成断言，合同 ACL-SLICE-01 WU02 expected_evidence）。
 *
 * <p>以 H2（MySQL 模式）真实执行 {@code V1__mcp_agent_tables.sql}，然后通过 JDBC 元数据断言
 * 六表存在、关键唯一约束 / 索引 / {@code credential_ref} 密文引用字段存在，
 * 以及 tool_registry 白名单恰好 5 个只读工具行（SEC-09，契约第 2 节）。</p>
 *
 * <p>说明：本地无 MySQL，按 YSS 约定以 H2 MySQL 模式做可本地验证的 DDL 结构断言；
 * 生产 DDL 语法口径以 MySQL 8 为准（D1 人工评审）。</p>
 */
class SchemaConstraintTest {

    /** 六表清单（数据架构 §5 / 迁移拆分：01 切片）。 */
    private static final List<String> EXPECTED_TABLES =
            Arrays.asList("agent", "agent_credential", "agent_domain", "mcp_session", "tool_registry", "audit_log");

    /** tool_registry 白名单恰好 5 个只读工具（冻结契约第 2 节；顺序无关，按名称比较）。 */
    private static final List<String> EXPECTED_TOOLS =
            Arrays.asList("search_assets", "asset_detail", "lineage", "impact_analysis", "classification_query");

    private static final String DDL_RESOURCE = "/db/migration/V1__mcp_agent_tables.sql";

    private static Connection connection;

    @BeforeAll
    static void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:acl_schema_constraint;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        executeDdl(connection, loadDdl());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void ddlCreatesAllSixTables() throws SQLException {
        Set<String> tables = listTables(connection);
        for (String table : EXPECTED_TABLES) {
            assertTrue(tables.contains(table), "V1 DDL 应创建表: " + table);
        }
    }

    @Test
    void uniqueConstraintsAndPrimaryKeysArePresent() throws SQLException {
        // agent_credential：(agent_id, credential_version) 唯一（数据架构 §5）
        assertUniqueIndexColumns(connection, "agent_credential", "uk_agent_credential_agent_version",
                new LinkedHashSet<>(Arrays.asList("agent_id", "credential_version")));
        // agent_domain：(agent_id, domain) 唯一
        assertUniqueIndexColumns(connection, "agent_domain", "uk_agent_domain_agent_domain",
                new LinkedHashSet<>(Arrays.asList("agent_id", "domain")));
        // audit_log：mcp_request_id 唯一（一次调用一行可复现，SEC-06/08）
        assertUniqueIndexColumns(connection, "audit_log", "uk_audit_log_mcp_request_id",
                new LinkedHashSet<>(Collections.singleton("mcp_request_id")));
        // tool_registry：tool_name 为主键（白名单主键）
        assertPrimaryKey(connection, "tool_registry", "tool_name");
        // agent：id 主键；agent_credential：id 主键；mcp_session：id 主键；audit_log：id 主键
        for (String table : Arrays.asList("agent", "agent_credential", "agent_domain", "mcp_session", "audit_log")) {
            assertPrimaryKey(connection, table, "id");
        }
    }

    @Test
    void keyIndexesArePresent() throws SQLException {
        Map<String, List<String>> indexes = indexNameToColumns(connection, "agent_credential");
        assertIndexWithColumns(indexes, "idx_agent_credential_agent_status",
                new LinkedHashSet<>(Arrays.asList("agent_id", "status")));

        indexes = indexNameToColumns(connection, "agent");
        assertIndexWithColumns(indexes, "idx_agent_name", Collections.singleton("name"));

        indexes = indexNameToColumns(connection, "agent_domain");
        assertIndexWithColumns(indexes, "idx_agent_domain_domain", Collections.singleton("domain"));

        indexes = indexNameToColumns(connection, "mcp_session");
        assertIndexWithColumns(indexes, "idx_mcp_session_agent_status",
                new LinkedHashSet<>(Arrays.asList("agent_id", "status")));
        assertIndexWithColumns(indexes, "idx_mcp_session_expires_at", Collections.singleton("expires_at"));

        indexes = indexNameToColumns(connection, "audit_log");
        assertIndexWithColumns(indexes, "idx_audit_log_session_id", Collections.singleton("session_id"));
        assertIndexWithColumns(indexes, "idx_audit_log_agent_timestamp",
                new LinkedHashSet<>(Arrays.asList("agent_id", "timestamp")));
        assertIndexWithColumns(indexes, "idx_audit_log_timestamp", Collections.singleton("timestamp"));
        assertIndexWithColumns(indexes, "idx_audit_log_result_code", Collections.singleton("result_code"));
    }

    @Test
    void credentialRefColumnIsKmsReferenceField() throws SQLException {
        Map<String, Integer> columns = columnNameToJdbcType(connection, "agent_credential");
        assertTrue(columns.containsKey("credential_ref"),
                "agent_credential 应包含 credential_ref（KMS 密文引用字段，SEC-05/D3）");
        assertEquals(java.sql.Types.VARCHAR, columns.get("credential_ref"),
                "credential_ref 应为字符串类型（varchar），实际 JDBC 类型: " + columns.get("credential_ref"));
    }

    @Test
    void toolRegistryWhitelistHasExactlyFiveReadOnlyTools() throws SQLException {
        List<String> tools = queryStringList(connection, "SELECT tool_name FROM tool_registry ORDER BY tool_name");
        assertEquals(5, tools.size(), "tool_registry 白名单应恰好 5 行（SEC-09）");
        List<String> sortedExpected = EXPECTED_TOOLS.stream().sorted().collect(Collectors.toList());
        assertEquals(sortedExpected, tools, "白名单工具名应与冻结契约第 2 节完全一致");
    }

    // ----------------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------------

    private static String loadDdl() throws IOException {
        try (InputStream in = SchemaConstraintTest.class.getResourceAsStream(DDL_RESOURCE)) {
            assertNotNull(in, "未找到 DDL 资源: " + DDL_RESOURCE);
            StringBuilder ddl = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("--")) {
                        continue;
                    }
                    ddl.append(line).append('\n');
                }
            }
            return ddl.toString();
        }
    }

    private static void executeDdl(Connection conn, String ddl) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            for (String stmt : ddl.split(";")) {
                if (stmt.trim().isEmpty()) {
                    continue;
                }
                statement.execute(stmt);
            }
        }
    }

    private static Set<String> listTables(Connection conn) throws SQLException {
        Set<String> tables = new LinkedHashSet<>();
        try (ResultSet rs = conn.getMetaData().getTables(null, null, null, new String[] {"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        return tables;
    }

    private static Map<String, List<String>> indexNameToColumns(Connection conn, String table) throws SQLException {
        Map<String, List<String>> indexColumns = new TreeMap<>();
        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null) {
                    continue;
                }
                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
            }
        }
        return indexColumns;
    }

    private static Map<String, Integer> columnNameToJdbcType(Connection conn, String table) throws SQLException {
        Map<String, Integer> columns = new TreeMap<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, "%")) {
            while (rs.next()) {
                columns.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getInt("DATA_TYPE"));
            }
        }
        return columns;
    }

    private static List<String> queryStringList(Connection conn, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        return values;
    }

    private static void assertUniqueIndexColumns(Connection conn, String table, String indexPrefix, Set<String> expectedColumns)
            throws SQLException {
        Map<String, List<String>> allIndexes = indexNameToColumns(conn, table);
        boolean found = false;
        for (Map.Entry<String, List<String>> entry : allIndexes.entrySet()) {
            String indexName = entry.getKey();
            // H2（MySQL 模式）为唯一约束自动生成的索引名可能带 _INDEX_xx 后缀，按前缀匹配
            if (indexName != null && indexName.toLowerCase().contains(indexPrefix.toLowerCase())
                    && new LinkedHashSet<>(entry.getValue()).equals(expectedColumns)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "表 " + table + " 应存在唯一约束 " + indexPrefix + "，列 = " + expectedColumns
                + "，实际索引: " + allIndexes);
    }

    private static void assertIndexWithColumns(Map<String, List<String>> indexes, String indexName, Set<String> expectedColumns) {
        assertTrue(indexes.containsKey(indexName), "应存在索引 " + indexName + "，实际: " + indexes.keySet());
        assertEquals(expectedColumns, new LinkedHashSet<>(indexes.get(indexName)),
                "索引 " + indexName + " 列应一致");
    }

    private static void assertPrimaryKey(Connection conn, String table, String expectedColumn) throws SQLException {
        List<String> pkColumns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, table)) {
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"));
            }
        }
        assertEquals(1, pkColumns.size(), "表 " + table + " 主键应恰好 1 列，实际: " + pkColumns);
        assertEquals(expectedColumn.toLowerCase(), pkColumns.get(0).toLowerCase(),
                "表 " + table + " 主键列应为 " + expectedColumn);
    }
}
