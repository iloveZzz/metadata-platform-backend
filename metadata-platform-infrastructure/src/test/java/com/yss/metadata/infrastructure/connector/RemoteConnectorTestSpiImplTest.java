package com.yss.metadata.infrastructure.connector;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.datasource.ConnectionTestVO;
import com.yss.datamiddleds.client.feign.ConnectionTestFeignClient;
import com.yss.metadata.domain.connector.model.*;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RemoteConnectorTestSpiImpl 连通性测试适配器测试。
 */
@ExtendWith(MockitoExtension.class)
class RemoteConnectorTestSpiImplTest {

    @Mock
    private ConnectionTestFeignClient connectionTestFeignClient;

    private MetadataCollectorConvertor convertor;
    private RemoteConnectorTestSpiImpl connectorTestSpi;

    @BeforeEach
    void setUp() {
        convertor = Mappers.getMapper(MetadataCollectorConvertor.class);
        connectorTestSpi = new RemoteConnectorTestSpiImpl(connectionTestFeignClient, convertor);
    }

    @Test
    @DisplayName("GaussDB 方言连接：未通过 PoC 认证直接返回方言错误（DIALECT）")
    void test_GaussDb_ReturnsDialectFailure() {
        Connector connector = buildConnector("c-gauss", ConnectorType.GAUSSDB, Dialect.GAUSSDB);

        ConnectTestResult result = connectorTestSpi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.DIALECT);
        assertThat(result.getMessage()).contains("GaussDB 方言连接尚未通过 PoC 认证");
    }

    @Test
    @DisplayName("远端连通性测试成功：返回 isConnected=true")
    void test_Success_ReturnsConnected() {
        Connector connector = buildConnector("ds-10", ConnectorType.MYSQL, Dialect.NATIVE);

        ConnectionTestVO vo = new ConnectionTestVO();
        vo.setSuccess(true);
        vo.setMessage("连接成功");

        when(connectionTestFeignClient.testDataSourceConnection(eq("ds-10")))
                .thenReturn(SingleResult.of(vo));

        ConnectTestResult result = connectorTestSpi.test(connector);

        assertThat(result.isConnected()).isTrue();
        assertThat(result.getMessage()).isEqualTo("连接成功");
        assertThat(result.getErrorType()).isNull();
    }

    @Test
    @DisplayName("远端密码错误：分类映射为 CREDENTIAL 错误")
    void test_CredentialFailure_ReturnsCredentialError() {
        Connector connector = buildConnector("ds-10", ConnectorType.MYSQL, Dialect.NATIVE);

        ConnectionTestVO vo = new ConnectionTestVO();
        vo.setSuccess(false);
        vo.setErrorCategory("AUTH");
        vo.setMessage("Access denied for user 'root'@'%'");

        when(connectionTestFeignClient.testDataSourceConnection(eq("ds-10")))
                .thenReturn(SingleResult.of(vo));

        ConnectTestResult result = connectorTestSpi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.CREDENTIAL);
        assertThat(result.getMessage()).contains("Access denied");
    }

    @Test
    @DisplayName("RPC 调用超时或抛出异常：捕获并归类为 NETWORK 错误")
    void test_FeignException_ReturnsNetworkError() {
        Connector connector = buildConnector("ds-10", ConnectorType.MYSQL, Dialect.NATIVE);

        when(connectionTestFeignClient.testDataSourceConnection(eq("ds-10")))
                .thenThrow(new RuntimeException("Connection timed out after 5000ms"));

        ConnectTestResult result = connectorTestSpi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);
        assertThat(result.getMessage()).contains("数据源服务通信异常");
    }

    private Connector buildConnector(String id, ConnectorType type, Dialect dialect) {
        return Connector.builder()
                .id(id)
                .name("测试连接器")
                .type(type)
                .dialect(dialect)
                .host("127.0.0.1")
                .port(3306)
                .username("root")
                .credentialRef("enc_pwd")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.CONNECTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
