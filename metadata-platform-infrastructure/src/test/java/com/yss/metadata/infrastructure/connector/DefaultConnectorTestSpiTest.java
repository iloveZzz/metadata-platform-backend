package com.yss.metadata.infrastructure.connector;

import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认连接测试适配器行为测试（WU-01-01）。
 *
 * <p>合同 seam_deferred：GaussDB 方言连接 PoC 未认证，必须明确提示、不伪装支持；
 * 其他类型物理连接为 seam-deferred，返回明确提示而非伪成功。</p>
 */
class DefaultConnectorTestSpiTest {

    private DefaultConnectorTestSpi spi;

    @BeforeEach
    void setUp() {
        spi = new DefaultConnectorTestSpi();
    }

    @Test
    @DisplayName("GaussDB 类型连接测试返回方言错误且明确提示未认证")
    void gaussDbTypeReturnsDialectFailure() {
        Connector connector = buildConnector(ConnectorType.GAUSSDB, Dialect.GAUSSDB);

        ConnectTestResult result = spi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.DIALECT);
        assertThat(result.getMessage()).contains("PoC").contains("不支持");
    }

    @Test
    @DisplayName("gaussdb 方言（非 GaussDB 类型）同样返回方言错误")
    void gaussDbDialectReturnsDialectFailure() {
        Connector connector = buildConnector(ConnectorType.MYSQL, Dialect.GAUSSDB);

        ConnectTestResult result = spi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.DIALECT);
    }

    @Test
    @DisplayName("其他类型物理连接为 seam-deferred，返回明确提示而非伪成功")
    void otherTypesReturnDeferredFailure() {
        Connector connector = buildConnector(ConnectorType.MYSQL, Dialect.NATIVE);

        ConnectTestResult result = spi.test(connector);

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);
        assertThat(result.getMessage()).contains("seam-deferred");
    }

    private Connector buildConnector(ConnectorType type, Dialect dialect) {
        return Connector.builder()
                .id("c-1")
                .name("测试库")
                .type(type)
                .host("10.0.0.1")
                .port(3306)
                .dialect(dialect)
                .username("root")
                .credentialRef("enc:v1:ref")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
