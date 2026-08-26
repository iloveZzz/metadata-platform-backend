package com.yss.metadata.repository;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import com.yss.metadata.repository.entity.CollectorTaskPO;
import com.yss.metadata.repository.entity.ConnectorPO;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * H2 内存库 + MyBatis（yss BaseRepository / MP MybatisConfiguration）测试基座。
 *
 * <p>持久化集成验证 seam：用 H2 代替真实 MySQL 验证 GatewayImpl 的
 * PO/Mapper（EntityProvider 注解 SQL）读写链路；生产 MySQL 需连接真实库验证。</p>
 */
public abstract class H2MapperTestSupport {

    protected static SqlSessionFactory sqlSessionFactory;

    protected SqlSession sqlSession;

    @BeforeAll
    static void initSqlSessionFactory() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:metadata_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        try (Connection connection = dataSource.getConnection()) {
            RunScript.execute(connection, new InputStreamReader(
                    H2MapperTestSupport.class.getResourceAsStream("/schema-h2.sql"), StandardCharsets.UTF_8));
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("h2-test", new JdbcTransactionFactory(), dataSource));
        // 分页插件（对齐生产 YssDataMybatisConfig），支撑 selectPage 生成 LIMIT 与 count
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(ConnectorRepository.class);
        configuration.addMapper(CollectorTaskRepository.class);
        configuration.addMapper(CollectorInstanceRepository.class);
        configuration.addMapper(AssetRepository.class);
        configuration.addMapper(AssetColumnRepository.class);
        configuration.addMapper(AssetVersionRepository.class);
        configuration.addMapper(AssetFavoriteRepository.class);
        configuration.addMapper(AssetTagRepository.class);
        configuration.addMapper(LineageEdgeRepository.class);
        configuration.addMapper(ExportTaskRepository.class);
        configuration.addMapper(AuditLogRepository.class);
        configuration.addMapper(LineageImpactMapper.class);
        configuration.addMapper(ClassRuleRepository.class);
        configuration.addMapper(ClassificationRepository.class);
        configuration.addMapper(PropagateTaskRepository.class);
        configuration.addMapper(IntegrationConfigRepository.class);
        configuration.addMapper(OpenLineageEventRepository.class);
        configuration.addMapper(RoleRepository.class);
        configuration.addMapper(RoleDomainRepository.class);
        configuration.addMapper(DataDomainRepository.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @BeforeEach
    void openSession() {
        sqlSession = sqlSessionFactory.openSession(true);
        cleanTables();
    }

    /**
     * 每个用例前清空全部业务表，保证用例间状态隔离
     * （H2 内存库 DB_CLOSE_DELAY=-1 跨用例共享，必须显式清理）。
     */
    private void cleanTables() {
        try (java.sql.Statement statement = sqlSession.getConnection().createStatement()) {
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.execute("TRUNCATE TABLE lineage_edge");
            statement.execute("TRUNCATE TABLE export_task");
            statement.execute("TRUNCATE TABLE audit_log");
            statement.execute("TRUNCATE TABLE asset_favorite");
            statement.execute("TRUNCATE TABLE asset_tag");
            statement.execute("TRUNCATE TABLE asset_version");
            statement.execute("TRUNCATE TABLE asset_column");
            statement.execute("TRUNCATE TABLE asset");
            statement.execute("TRUNCATE TABLE collector_task");
            statement.execute("TRUNCATE TABLE collector_instance");
            statement.execute("TRUNCATE TABLE data_source");
            statement.execute("TRUNCATE TABLE classification");
            statement.execute("TRUNCATE TABLE class_rule");
            statement.execute("TRUNCATE TABLE propagate_task");
            statement.execute("TRUNCATE TABLE integration_config");
            statement.execute("TRUNCATE TABLE openlineage_event");
            statement.execute("TRUNCATE TABLE role_domain");
            statement.execute("TRUNCATE TABLE role");
            statement.execute("TRUNCATE TABLE data_domain");
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("清理 H2 测试表失败", e);
        }
    }

    @AfterEach
    void closeSession() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    protected ConnectorPO buildConnectorPo(String id, String name) {
        ConnectorPO po = new ConnectorPO();
        po.setId(id);
        po.setName(name);
        po.setType("MySQL");
        po.setHost("10.0.0.1");
        po.setPort(3306);
        po.setDialect("native");
        po.setUsername("root");
        po.setCredentialRef("seam-base64:cHJk");
        po.setAutoClassify(Boolean.TRUE);
        po.setStatus("draft");
        po.setCreatedAt(java.time.LocalDateTime.of(2026, 8, 1, 0, 0, 0));
        po.setUpdatedAt(java.time.LocalDateTime.of(2026, 8, 1, 0, 0, 0));
        return po;
    }

    protected CollectorTaskPO buildTaskPo(String id, String connectorId, String schedule) {
        CollectorTaskPO po = new CollectorTaskPO();
        po.setId(id);
        po.setName("每日采集");
        po.setConnectorId(connectorId);
        po.setSchedule(schedule);
        po.setMode("incremental");
        po.setStrategy("ignore");
        po.setAutoClassify(Boolean.TRUE);
        po.setStatus("pending");
        po.setCreatedAt(java.time.LocalDateTime.of(2026, 8, 1, 0, 0, 0));
        po.setUpdatedAt(java.time.LocalDateTime.of(2026, 8, 1, 0, 0, 0));
        return po;
    }

    protected AssetPO buildAssetPo(String id, String sourceId, String name, String type,
                                   String domain, String owner, String classification,
                                   String status, java.time.LocalDateTime updatedAt) {
        AssetPO po = new AssetPO();
        po.setId(id);
        po.setSourceId(sourceId);
        po.setName(name);
        po.setType(type);
        po.setDomain(domain);
        po.setOwner(owner);
        po.setClassification(classification);
        po.setStatus(status);
        po.setIsExcluded(false);
        po.setUpdatedAt(updatedAt);
        return po;
    }
}
