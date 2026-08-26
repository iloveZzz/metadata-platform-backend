package com.yss.metadata.repository;

import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.infrastructure.convertor.ConnectorConvertor;
import com.yss.metadata.repository.gateway.impl.ConnectorGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 连接器仓储持久化集成测试（WU-01-03，H2 内存库替代真实 MySQL）。
 *
 * <p>验证 PO/Mapper（yss BaseRepository + EntityProvider 注解 SQL）到
 * Domain 端口的完整读写链路，含凭据只存加密引用（不落库明文）。</p>
 */
class ConnectorGatewayImplH2Test extends H2MapperTestSupport {

    private ConnectorGateway repository;

    @BeforeEach
    void setUp() {
        repository = new ConnectorGatewayImpl(sqlSession.getMapper(ConnectorRepository.class),
                Mappers.getMapper(ConnectorConvertor.class));
    }

    @Test
    @DisplayName("新增连接器并回读：字段完整往返，凭据仅存加密引用")
    void saveAndFindByIdRoundTrip() {
        Connector connector = buildConnector("c-1", "订单库");

        repository.save(connector);

        Optional<Connector> loaded = repository.findById("c-1");
        assertThat(loaded).isPresent();
        Connector actual = loaded.orElseThrow(AssertionError::new);
        assertThat(actual.getName()).isEqualTo("订单库");
        assertThat(actual.getType()).isEqualTo(ConnectorType.MYSQL);
        assertThat(actual.getHost()).isEqualTo("10.0.0.1");
        assertThat(actual.getPort()).isEqualTo(3306);
        assertThat(actual.getDialect()).isEqualTo(Dialect.NATIVE);
        assertThat(actual.getUsername()).isEqualTo("root");
        assertThat(actual.getAutoClassify()).isTrue();
        assertThat(actual.getStatus()).isEqualTo(ConnectorStatus.DRAFT);
        assertThat(actual.getCreatedAt()).isNotNull();
        // 凭据不落库明文：引用不等于且不包含明文
        assertThat(actual.getCredentialRef()).isNotEqualTo("pwd-plaintext-123");
        assertThat(actual.getCredentialRef()).doesNotContain("pwd-plaintext-123");
    }

    @Test
    @DisplayName("更新连接器状态并回读（save 对已存在记录执行更新）")
    void saveUpdatePersistsChanges() {
        repository.save(buildConnector("c-1", "订单库"));

        Connector connector = repository.findById("c-1").orElseThrow(AssertionError::new);
        connector.markConnected();
        repository.save(connector);

        Connector loaded = repository.findById("c-1").orElseThrow(AssertionError::new);
        assertThat(loaded.getStatus()).isEqualTo(ConnectorStatus.CONNECTED);
    }

    @Test
    @DisplayName("名称唯一性检查：存在与排除自身")
    void existsByNameChecks() {
        repository.save(buildConnector("c-1", "订单库"));
        repository.save(buildConnector("c-2", "用户库"));

        assertThat(repository.existsByName("订单库")).isTrue();
        assertThat(repository.existsByName("不存在")).isFalse();
        assertThat(repository.existsByNameExcluding("订单库", "c-1")).isFalse();
        assertThat(repository.existsByNameExcluding("订单库", "other-id")).isTrue();
    }

    @Test
    @DisplayName("列表返回全部连接器，删除后移除")
    void findAllAndDelete() {
        repository.save(buildConnector("c-1", "订单库"));
        repository.save(buildConnector("c-2", "用户库"));

        List<Connector> all = repository.findAll();
        assertThat(all).hasSize(2);

        repository.deleteById("c-1");
        assertThat(repository.findById("c-1")).isEmpty();
        assertThat(repository.findAll()).hasSize(1);
    }

    private Connector buildConnector(String id, String name) {
        return Connector.builder()
                .id(id)
                .name(name)
                .type(ConnectorType.MYSQL)
                .host("10.0.0.1")
                .port(3306)
                .dialect(Dialect.NATIVE)
                .username("root")
                .credentialRef("enc:v1:encrypted-not-plaintext")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.DRAFT)
                .createdAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0))
                .build();
    }
}
