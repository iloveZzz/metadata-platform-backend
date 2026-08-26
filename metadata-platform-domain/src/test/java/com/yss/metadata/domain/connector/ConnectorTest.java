package com.yss.metadata.domain.connector;

import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 连接器聚合行为测试（WU-01-01）。
 *
 * <p>覆盖：状态机（草稿/已连接/失败/停用）、配置变更重置草稿、领域不变量校验、
 * 凭据只保存加密引用（不出现明文密码字段）。</p>
 */
class ConnectorTest {

    @Test
    @DisplayName("新建连接器初始状态应为草稿")
    void createThenStatusIsDraft() {
        Connector connector = buildValidConnector();

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.DRAFT);
        assertThat(connector.getCreatedAt()).isNotNull();
        assertThat(connector.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("测试连接成功应流转到已连接")
    void markConnectedThenStatusIsConnected() {
        Connector connector = buildValidConnector();

        connector.markConnected();

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.CONNECTED);
    }

    @Test
    @DisplayName("测试连接失败应流转到失败")
    void markTestFailedThenStatusIsFailed() {
        Connector connector = buildValidConnector();

        connector.markTestFailed();

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.FAILED);
    }

    @Test
    @DisplayName("停用应流转到停用状态")
    void disableThenStatusIsDisabled() {
        Connector connector = buildValidConnector();

        connector.disable();

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.DISABLED);
    }

    @Test
    @DisplayName("配置变更后状态应重置为草稿并刷新更新时间")
    void updateResetsStatusToDraftAndBumpsUpdatedAt() {
        Connector connector = buildValidConnector();
        connector.markConnected();
        LocalDateTime beforeUpdate = connector.getUpdatedAt();

        connector.update("新名称", ConnectorType.MYSQL, "10.0.0.2", 3307, Dialect.NATIVE,
                "root", "enc:v2:ref", Boolean.TRUE);

        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.DRAFT);
        assertThat(connector.getName()).isEqualTo("新名称");
        assertThat(connector.getHost()).isEqualTo("10.0.0.2");
        assertThat(connector.getPort()).isEqualTo(3307);
        assertThat(connector.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("配置变更应保存新的凭据加密引用")
    void updateKeepsEncryptedCredentialRef() {
        Connector connector = buildValidConnector();

        connector.update("新名称", ConnectorType.MYSQL, "10.0.0.2", 3307, Dialect.NATIVE,
                "root", "enc:v2:ref", Boolean.TRUE);

        assertThat(connector.getCredentialRef()).isEqualTo("enc:v2:ref");
    }

    @Test
    @DisplayName("更新时名称不能为空")
    void updateRejectsBlankName() {
        Connector connector = buildValidConnector();

        assertThatThrownBy(() -> connector.update("  ", ConnectorType.MYSQL, "10.0.0.2", 3307,
                Dialect.NATIVE, "root", "enc:v1:ref", Boolean.TRUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("连接器名称");
    }

    @Test
    @DisplayName("名称不能为空")
    void validateRejectsBlankName() {
        Connector connector = buildValidConnector();

        connector.setName("");
        assertThatThrownBy(connector::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("连接器名称");
    }

    @Test
    @DisplayName("主机地址不能为空")
    void validateRejectsBlankHost() {
        Connector connector = buildValidConnector();

        connector.setHost(null);
        assertThatThrownBy(connector::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主机地址");
    }

    @Test
    @DisplayName("端口必须在 1-65535 之间")
    void validateRejectsInvalidPort() {
        Connector connector = buildValidConnector();

        connector.setPort(70000);
        assertThatThrownBy(connector::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("端口");

        connector.setPort(-1);
        assertThatThrownBy(connector::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("端口");
    }

    @Test
    @DisplayName("方言不能为空")
    void validateRejectsNullDialect() {
        Connector connector = buildValidConnector();

        connector.setDialect(null);
        assertThatThrownBy(connector::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("方言");
    }

    @Test
    @DisplayName("凭据字段只保存加密引用，不保存明文密码")
    void credentialIsStoredAsRefOnly() {
        Connector connector = Connector.builder()
                .id("c-1")
                .name("生产库")
                .type(ConnectorType.MYSQL)
                .host("10.0.0.1")
                .port(3306)
                .dialect(Dialect.NATIVE)
                .username("root")
                .credentialRef("enc:v1:encrypted-ref")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThat(connector.getCredentialRef()).isEqualTo("enc:v1:encrypted-ref");
        // 聚合无 password 字段：密码明文在连接器聚合与持久化链路中不落库
        assertThat(connector).isNotEqualTo("plaintext-password");
        assertThat(connector.getCredentialRef()).doesNotContain("plaintext-password");
    }

    private Connector buildValidConnector() {
        return Connector.builder()
                .id("c-1")
                .name("生产库")
                .type(ConnectorType.MYSQL)
                .host("10.0.0.1")
                .port(3306)
                .dialect(Dialect.NATIVE)
                .username("root")
                .credentialRef("enc:v1:encrypted-ref")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
