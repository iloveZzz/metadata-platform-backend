package com.yss.datamiddle.dqinsight;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Web 契约测试配置：H2（MySQL 模式）内存库 + Liquibase 迁移（db/changelog 建表脚本）。
 *
 * <p>外部依赖（通道凭证存储 / 资产防腐层 / 健康分计算触发）在测试中以 @MockBean 替换。</p>
 * <p>内存库名按 ApplicationContext 唯一化（${random.uuid}）：带 @MockBean 的测试类会形成独立上下文
 * 并触发 Liquibase 二次初始化；独立库名避免 DATABASECHANGELOG 已存在冲突（2026-08-13，A3-AM-03）。</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.yss.datamiddle.dqinsight")
public class DqInsightTestApplication {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url(env.resolvePlaceholders(
                        "jdbc:h2:mem:dqwebtest_${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"))
                .username("sa")
                .password("")
                .build();
    }
}
