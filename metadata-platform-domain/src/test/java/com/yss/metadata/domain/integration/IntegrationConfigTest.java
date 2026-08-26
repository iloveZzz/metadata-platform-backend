package com.yss.metadata.domain.integration;

import com.yss.metadata.domain.integration.model.DataHubEndpoint;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成配置领域模型测试（WU-05-01）。
 *
 * <p>覆盖：单例行 id 约定、端点值对象（防腐层输入：仅端点+认证，隔离外部模型）。</p>
 */
class IntegrationConfigTest {

    @Test
    @DisplayName("单例配置行约定：id 固定为 1，缺省 gravitinoEnabled=false")
    void singletonRowConvention() {
        assertThat(IntegrationConfig.SINGLETON_ID).isEqualTo("1");

        IntegrationConfig config = IntegrationConfig.builder().build();
        assertThat(config.getId()).isNull();
        assertThat(config.getGravitinoEnabled()).isNull();
        assertThat(config.getGravitinoEndpoint()).isNull();
        assertThat(config.getDatahubEndpoint()).isNull();
    }

    @Test
    @DisplayName("配置完整构建：端点/认证引用/最近测试/更新时间均可承载")
    void fullConfigBuild() {
        IntegrationConfig config = IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEndpoint("http://gravitino:8090")
                .gravitinoAuthRef("seam-base64:xxx")
                .gravitinoEnabled(true)
                .gravitinoLastTest("2026-08-12T10:00 连接测试通过")
                .datahubEndpoint("http://datahub:8080")
                .datahubAuthRef("seam-base64:yyy")
                .updatedAt(LocalDateTime.of(2026, 8, 12, 10, 0, 0))
                .build();

        assertThat(config.getGravitinoEndpoint()).isEqualTo("http://gravitino:8090");
        assertThat(config.getGravitinoAuthRef()).isNotBlank();
        assertThat(config.getGravitinoEnabled()).isTrue();
        assertThat(config.getGravitinoLastTest()).contains("连接测试通过");
        assertThat(config.getDatahubEndpoint()).isEqualTo("http://datahub:8080");
        assertThat(config.getDatahubAuthRef()).isNotBlank();
        assertThat(config.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Gravitino 端点值对象：防腐层只暴露端点与认证令牌")
    void gravitinoEndpointValueObject() {
        GravitinoEndpoint endpoint = GravitinoEndpoint.builder()
                .endpoint("http://gravitino:8090")
                .authToken("token-abc")
                .build();

        assertThat(endpoint.getEndpoint()).isEqualTo("http://gravitino:8090");
        assertThat(endpoint.getAuthToken()).isEqualTo("token-abc");
    }

    @Test
    @DisplayName("DataHub 端点值对象：防腐层只暴露端点与认证令牌")
    void datahubEndpointValueObject() {
        DataHubEndpoint endpoint = DataHubEndpoint.builder()
                .endpoint("http://datahub:8080")
                .authToken("token-xyz")
                .build();

        assertThat(endpoint.getEndpoint()).isEqualTo("http://datahub:8080");
        assertThat(endpoint.getAuthToken()).isEqualTo("token-xyz");
    }
}
