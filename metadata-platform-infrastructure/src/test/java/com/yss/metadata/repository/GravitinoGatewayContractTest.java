package com.yss.metadata.repository;

import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.DataHubExportResult;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;
import com.yss.metadata.repository.gateway.impl.DefaultDataHubExporter;
import com.yss.metadata.repository.gateway.impl.DefaultGravitinoGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防腐层契约测试（WU-05-03）。
 *
 * <p>seam-deferred 契约：默认实现诚实返回失败（分类提示），不伪装接入；
 * 防腐层只暴露端点/认证（外部模型隔离由端口签名约束，编译期保证）。</p>
 */
class GravitinoGatewayContractTest {

    private final DefaultGravitinoGateway gravitinoGateway = new DefaultGravitinoGateway();
    private final DefaultDataHubExporter dataHubExporter = new DefaultDataHubExporter();

    @Test
    @DisplayName("Gravitino 测试连接：未配置端点返回 network 分类失败")
    void gravitinoWithoutEndpointClassified() {
        ConnectTestResult result = gravitinoGateway.testConnection(GravitinoEndpoint.builder().build());

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);
        assertThat(result.getMessage()).contains("Gravitino");
    }

    @Test
    @DisplayName("Gravitino 测试连接：已配置端点 seam-deferred 诚实返回 network 失败，不伪装成功")
    void gravitinoSeamDeferredHonestFailure() {
        ConnectTestResult result = gravitinoGateway.testConnection(
                GravitinoEndpoint.builder().endpoint("http://gravitino:8090").authToken("token").build());

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);
        assertThat(result.getMessage()).contains("seam-deferred");
    }

    @Test
    @DisplayName("DataHub 导出：seam-deferred 诚实返回失败，不伪装导出成功")
    void datahubSeamDeferredHonestFailure() {
        DataHubExportResult result = dataHubExporter.export(
                DataHubEndpoint.builder().endpoint("http://datahub:8080").authToken("token").build(),
                "u-me");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("seam-deferred");
    }

    @Test
    @DisplayName("防腐层端口签名：外部模型不泄露（端点值对象仅承载 endpoint/authToken）")
    void anticorruptionBoundary() {
        GravitinoEndpoint gravitino = GravitinoEndpoint.builder()
                .endpoint("http://gravitino:8090")
                .authToken("t")
                .build();
        assertThat(gravitino.getEndpoint()).isNotBlank();
        assertThat(gravitino.getAuthToken()).isNotBlank();

        DataHubEndpoint datahub = DataHubEndpoint.builder()
                .endpoint("http://datahub:8080")
                .authToken("t")
                .build();
        assertThat(datahub.getEndpoint()).isNotBlank();
        assertThat(datahub.getAuthToken()).isNotBlank();
    }
}
