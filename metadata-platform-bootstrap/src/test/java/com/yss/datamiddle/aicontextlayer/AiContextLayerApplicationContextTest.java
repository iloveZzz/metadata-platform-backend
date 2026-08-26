package com.yss.datamiddle.aicontextlayer;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver.JdbcCredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver.JdbcSessionRepository;
import com.yss.datamiddle.aicontextlayer.mcpserver.McpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * bootstrap 启动类冒烟测试（WU-01-01，WU-01-02 更新，WU-01-03/04 更新）：
 * Spring 上下文以真实配置加载。
 *
 * <p>WU-01-02（DDL + PO 层落地）后移除 MyBatis 自动配置排除项，数据源 / 建表由
 * 真实配置驱动：数据源由 YSS PrimaryDataSourceConfiguration 从
 * {@code spring.datasource.primary.*} 创建，MyBatis-Plus SqlSessionFactory 与
 * Repository Mapper 扫描（yss.mybatis.mapper-scan）正常装配。</p>
 *
 * <p>WU-01-03/04：InMemory 端口 seam 已由 DB-backed 实现替换（无冒充生产持久化）；
 * 装配断言验证 {@link JdbcCredentialVerificationGateway} 与 {@link JdbcSessionRepository}。</p>
 *
 * <p>本地无外部 MySQL，按 YSS 约定以 H2（MySQL 模式）内存库作为测试数据源
 * （测试专用，仅覆盖 spring.datasource.primary.*；不影响生产 datasource 配置）。
 * 生产数据源 URL / 时区等配置保持 application.yml 原样。</p>
 */
@SpringBootTest(classes = com.yss.metadata.MetadataPlatformApplication.class, properties = {
    // 测试数据源：H2 MySQL 模式（本地可验证；生产仍用 MySQL）
    "spring.datasource.primary.url=jdbc:h2:mem:ai_context_layer;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=",
    // YSS 数据源由 PrimaryDataSourceConfiguration 装配；排除 Spring Boot 通用 DataSourceAutoConfiguration，
    // 避免嵌入式数据源与 primaryDataSource 双数据源歧义
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.liquibase.enabled=false"
})
class AiContextLayerApplicationContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.containsBean("metadataPlatformApplication")).isTrue();
    }

    @Test
    void mcpServerSkeletonBeanIsWired() {
        assertThat(applicationContext.getBean(McpServer.class)).isNotNull();
    }

    @Test
    void yssDataSourceAndMybatisAreWiredWithRealConfig() {
        assertThat(applicationContext.containsBean("primaryDataSource")).isTrue();
        assertThat(applicationContext.containsBean("sqlSessionFactory")).isTrue();
        // WU-01-02：六表 Repository 经 @MapperScan（markerInterface = BasePlusRepository）注册为 Mapper Bean
        assertThat(applicationContext.containsBean("agentRepository")).isTrue();
        assertThat(applicationContext.containsBean("agentCredentialRepository")).isTrue();
        assertThat(applicationContext.containsBean("agentDomainRepository")).isTrue();
        assertThat(applicationContext.containsBean("mcpSessionRepository")).isTrue();
        assertThat(applicationContext.containsBean("toolRegistryRepository")).isTrue();
        assertThat(applicationContext.containsBean("aclAuditLogRepository")).isTrue();
        assertThat(applicationContext.containsBean("auditLogRepository")).isTrue();
    }

    @Test
    void portSeamsAreDbBackedNotInMemory() {
        // WU-01-03/04：InMemory seam 已删除并由 DB-backed 实现替换（无冒充生产持久化）
        CredentialVerificationGateway verificationGateway =
            applicationContext.getBean(CredentialVerificationGateway.class);
        assertThat(verificationGateway).isInstanceOf(JdbcCredentialVerificationGateway.class);
        SessionRepository sessionRepository = applicationContext.getBean(SessionRepository.class);
        assertThat(sessionRepository).isInstanceOf(JdbcSessionRepository.class);
    }

    @Test
    void credentialCipherBeanIsWiredWithDefaultFallbackKey() {
        com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher cipher =
            applicationContext.getBean(com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher.class);
        assertThat(cipher).isNotNull();
        String plaintext = "test-secret-value";
        String ref = cipher.reference(plaintext);
        assertThat(ref).startsWith("local:v1:");
        assertThat(cipher.dereference(ref)).isEqualTo(plaintext);
    }
}
