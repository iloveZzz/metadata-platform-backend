package com.yss.metadata;

import com.yss.datamiddleds.client.feign.ConnectionTestFeignClient;
import com.yss.datamiddleds.client.feign.DataSourceFeignClient;
import com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient;
import com.yss.metadata.domain.collector.spi.CollectorExecutionSpi;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;
import com.yss.metadata.infrastructure.collector.RemoteMetadataCollectorSpiImpl;
import com.yss.metadata.infrastructure.connector.RemoteConnectorTestSpiImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 DataMiddle DS Feign 客户端与 ACL 适配器在 Spring Boot 引导上下文中的自动装配。
 */
@SpringBootTest(classes = MetadataPlatformApplication.class, properties = {
        "spring.datasource.primary.url=jdbc:h2:mem:ds_client_bootstrap_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.primary.driver-class-name=org.h2.Driver",
        "spring.datasource.primary.username=sa",
        "spring.datasource.primary.password=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "spring.liquibase.enabled=false"
})
class DataMiddleDsClientBootstrapTest {

    @Autowired(required = false)
    private DatasourceMetadataFeignClient datasourceMetadataFeignClient;

    @Autowired(required = false)
    private ConnectionTestFeignClient connectionTestFeignClient;

    @Autowired(required = false)
    private DataSourceFeignClient dataSourceFeignClient;

    @Autowired(required = false)
    private com.yss.datamiddleds.client.feign.AppSystemFeignClient appSystemFeignClient;

    @Autowired
    private CollectorExecutionSpi collectorExecutionSpi;

    @Autowired
    private ConnectorTestSpi connectorTestSpi;

    @Test
    @DisplayName("验证 Feign 客户端 Bean 自动注册与注入成功")
    void testFeignClientsInjected() {
        assertThat(datasourceMetadataFeignClient).isNotNull();
        assertThat(connectionTestFeignClient).isNotNull();
        assertThat(dataSourceFeignClient).isNotNull();
        assertThat(appSystemFeignClient).isNotNull();
    }

    @Test
    @DisplayName("验证 ACL SPI 适配器成功装配为 Primary Bean")
    void testSpiAdaptersInjected() {
        assertThat(collectorExecutionSpi).isNotNull();
        assertThat(collectorExecutionSpi).isInstanceOf(RemoteMetadataCollectorSpiImpl.class);

        assertThat(connectorTestSpi).isNotNull();
        assertThat(connectorTestSpi).isInstanceOf(RemoteConnectorTestSpiImpl.class);
    }
}
