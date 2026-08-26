package com.yss.datamiddle.aicontextlayer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * 测试数据库支撑：在共享 H2（MySQL 模式）内存库上幂等执行 V1 DDL。
 *
 * <p>bootstrap 测试以 H2 内存库覆盖 {@code spring.datasource.primary.*}（本地可验证，
 * YSS 约定）；生产 MySQL 建表经 D1 人工评审的 V1 迁移脚本执行。本工具按
 * INFORMATION_SCHEMA 探测，已建表则跳过，保证跨测试类共享同一 schema。</p>
 */
public final class TestDbSupport {

    public static synchronized void ensureSchema(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'AUDIT_LOG' AND UPPER(COLUMN_NAME) = 'MCP_REQUEST_ID'",
            Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS audit_log");
            jdbcTemplate.execute("DROP TABLE IF EXISTS mcp_session");
            jdbcTemplate.execute("DROP TABLE IF EXISTS agent_domain");
            jdbcTemplate.execute("DROP TABLE IF EXISTS tool_registry");
            jdbcTemplate.execute("DROP TABLE IF EXISTS agent_credential");
            jdbcTemplate.execute("DROP TABLE IF EXISTS agent");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__mcp_agent_tables.sql"));
            populator.execute(jdbcTemplate.getDataSource());
        }
    }

    private TestDbSupport() {
    }
}
