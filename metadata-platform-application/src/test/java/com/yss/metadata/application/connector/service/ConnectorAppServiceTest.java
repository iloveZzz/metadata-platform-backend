package com.yss.metadata.application.connector.service;

import com.yss.metadata.application.connector.service.convertor.ConnectorAppConvertor;
import com.yss.metadata.application.connector.service.impl.ConnectorAppServiceImpl;
import com.yss.metadata.application.collector.support.InMemoryCollectorTaskRepository;
import com.yss.metadata.application.connector.support.FakeConnectorTestSpi;
import com.yss.metadata.application.connector.support.InMemoryConnectorRepository;
import com.yss.metadata.application.connector.support.TestCredentialCipher;
import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.dto.cmd.ConnectorUpdateCmd;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.connector.exception.ConnectTestException;
import com.yss.metadata.domain.connector.exception.ConnectorNameConflictException;
import com.yss.metadata.domain.connector.exception.ConnectorNotFoundException;
import com.yss.metadata.domain.connector.exception.ConnectorReferencedException;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 连接器应用服务用例测试（WU-01-01）。
 *
 * <p>覆盖：CRUD 编排、name 唯一冲突（409 语义）、连接测试错误分类
 * （network/credential/dialect）、凭据不落库明文、状态流转持久化。</p>
 */
class ConnectorAppServiceTest {

    private InMemoryConnectorRepository repository;
    private InMemoryCollectorTaskRepository taskRepository;
    private FakeConnectorTestSpi connectorTestSpi;
    private ConnectorAppService appService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryConnectorRepository();
        taskRepository = new InMemoryCollectorTaskRepository();
        connectorTestSpi = new FakeConnectorTestSpi();
        appService = new ConnectorAppServiceImpl(repository, taskRepository, connectorTestSpi,
                new TestCredentialCipher(), org.mapstruct.factory.Mappers.getMapper(ConnectorAppConvertor.class));
    }

    @Test
    @DisplayName("新增连接器成功：初始草稿、凭据加密引用、返回 VO")
    void createSuccess() {
        ConnectorAddCmd cmd = buildAddCmd("订单库", "10.0.0.1", 3306, "pwd-plaintext-123");

        ConnectorVO vo = appService.create(cmd);

        assertThat(vo.getId()).isNotBlank();
        assertThat(vo.getName()).isEqualTo("订单库");
        assertThat(vo.getType()).isEqualTo("MySQL");
        assertThat(vo.getHost()).isEqualTo("10.0.0.1");
        assertThat(vo.getPort()).isEqualTo(3306);
        assertThat(vo.getDialect()).isEqualTo("native");
        assertThat(vo.getUsername()).isEqualTo("root");
        assertThat(vo.getAutoClassify()).isTrue();
        assertThat(vo.getStatus()).isEqualTo("draft");

        Connector saved = repository.findById(vo.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getStatus()).isEqualTo(ConnectorStatus.DRAFT);
        // 凭据不落库明文：仅保存加密引用
        assertThat(saved.getCredentialRef()).isNotEqualTo("pwd-plaintext-123");
        assertThat(saved.getCredentialRef()).doesNotContain("pwd-plaintext-123");
    }

    @Test
    @DisplayName("不传密码时凭据引用保持为空")
    void createWithoutPasswordKeepsNullRef() {
        ConnectorAddCmd cmd = buildAddCmd("订单库", "10.0.0.1", 3306, null);

        ConnectorVO vo = appService.create(cmd);

        Connector saved = repository.findById(vo.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getCredentialRef()).isNull();
    }

    @Test
    @DisplayName("不传方言时默认 AUTO（冻结 OpenAPI dialect 可选）")
    void createWithoutDialectDefaultsAuto() {
        ConnectorAddCmd cmd = buildAddCmd("订单库", "10.0.0.1", 3306, null);
        cmd.setDialect(null);

        ConnectorVO vo = appService.create(cmd);

        assertThat(vo.getDialect()).isEqualTo("auto");
        Connector saved = repository.findById(vo.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getDialect()).isEqualTo(Dialect.AUTO);
    }

    @Test
    @DisplayName("新增重名连接器抛出名称冲突（409 语义）")
    void createDuplicateNameThrowsConflict() {
        appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));

        assertThatThrownBy(() -> appService.create(buildAddCmd("订单库", "10.0.0.2", 3307, "pwd")))
                .isInstanceOf(ConnectorNameConflictException.class)
                .hasMessageContaining("订单库");
    }

    @Test
    @DisplayName("连接器列表返回全部连接器")
    void listReturnsAll() {
        appService.create(buildAddCmd("库一", "10.0.0.1", 3306, "pwd"));
        appService.create(buildAddCmd("库二", "10.0.0.2", 3307, "pwd"));

        List<ConnectorVO> list = appService.list();

        assertThat(list).hasSize(2);
        assertThat(list).extracting(ConnectorVO::getName).containsExactlyInAnyOrder("库一", "库二");
    }

    @Test
    @DisplayName("更新连接器配置成功：字段生效、状态重置草稿、凭据引用不变")
    void updateSuccess() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "old-pwd"));
        String oldRef = repository.findById(created.getId()).orElseThrow(AssertionError::new).getCredentialRef();
        appService.testConnection(created.getId());

        ConnectorUpdateCmd updateCmd = new ConnectorUpdateCmd();
        updateCmd.setId(created.getId());
        updateCmd.setName("订单库-新");
        updateCmd.setType(ConnectorType.MYSQL);
        updateCmd.setHost("10.0.0.9");
        updateCmd.setPort(3307);
        updateCmd.setDialect(Dialect.NATIVE);
        updateCmd.setUsername("admin");
        updateCmd.setAutoClassify(Boolean.TRUE);

        ConnectorVO updated = appService.update(created.getId(), updateCmd);

        assertThat(updated.getName()).isEqualTo("订单库-新");
        assertThat(updated.getHost()).isEqualTo("10.0.0.9");
        assertThat(updated.getPort()).isEqualTo(3307);
        assertThat(updated.getStatus()).isEqualTo("draft");
        Connector saved = repository.findById(created.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getStatus()).isEqualTo(ConnectorStatus.DRAFT);
        // 未携带新密码时凭据引用保持不变
        assertThat(saved.getCredentialRef()).isEqualTo(oldRef);
    }

    @Test
    @DisplayName("更新携带新密码时重新加密并更新凭据引用")
    void updateWithNewPasswordReEncrypts() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "old-pwd"));
        String oldRef = repository.findById(created.getId()).orElseThrow(AssertionError::new).getCredentialRef();

        ConnectorUpdateCmd updateCmd = buildUpdateCmd(created.getId());
        updateCmd.setPassword("new-pwd-456");

        appService.update(created.getId(), updateCmd);

        Connector saved = repository.findById(created.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getCredentialRef()).isNotEqualTo(oldRef);
        assertThat(saved.getCredentialRef()).doesNotContain("new-pwd-456");
    }

    @Test
    @DisplayName("更新未携带方言时保留原方言（不静默改写）")
    void updateWithoutDialectKeepsExisting() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));
        assertThat(created.getDialect()).isEqualTo("native");

        ConnectorUpdateCmd updateCmd = buildUpdateCmd(created.getId());
        updateCmd.setDialect(null);
        updateCmd.setHost("10.0.0.9");

        ConnectorVO updated = appService.update(created.getId(), updateCmd);

        assertThat(updated.getDialect()).isEqualTo("native");
        Connector saved = repository.findById(created.getId()).orElseThrow(AssertionError::new);
        assertThat(saved.getDialect()).isEqualTo(Dialect.NATIVE);
    }

    @Test
    @DisplayName("更新不存在的连接器抛出未找到（404 语义）")
    void updateNotFoundThrows() {
        ConnectorUpdateCmd updateCmd = buildUpdateCmd("not-exist");

        assertThatThrownBy(() -> appService.update("not-exist", updateCmd))
                .isInstanceOf(ConnectorNotFoundException.class);
    }

    @Test
    @DisplayName("更新重名为其他连接器名称抛出冲突，保持自身名称不冲突")
    void updateDuplicateNameExcludingSelf() {
        ConnectorVO first = appService.create(buildAddCmd("库一", "10.0.0.1", 3306, "pwd"));
        appService.create(buildAddCmd("库二", "10.0.0.2", 3307, "pwd"));

        ConnectorUpdateCmd renameToExisting = buildUpdateCmd(first.getId());
        renameToExisting.setName("库二");
        assertThatThrownBy(() -> appService.update(first.getId(), renameToExisting))
                .isInstanceOf(ConnectorNameConflictException.class);

        ConnectorUpdateCmd keepOwnName = buildUpdateCmd(first.getId());
        keepOwnName.setName("库一");
        appService.update(first.getId(), keepOwnName);
        assertThat(repository.findById(first.getId())).isPresent();
    }

    @Test
    @DisplayName("删除连接器成功")
    void deleteRemoves() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));

        appService.delete(created.getId());

        assertThat(repository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("删除不存在的连接器抛出未找到（404 语义）")
    void deleteNotFoundThrows() {
        assertThatThrownBy(() -> appService.delete("not-exist"))
                .isInstanceOf(ConnectorNotFoundException.class);
    }

    @Test
    @DisplayName("连接器被采集任务引用时删除被拒绝（409 语义）")
    void deleteReferencedThrowsConflict() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));

        CollectorTask task = CollectorTask.builder()
                .id("t-1")
                .name("每日采集")
                .connectorId(created.getId())
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.INCREMENTAL)
                .strategy(CollectorStrategy.IGNORE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .build();
        taskRepository.save(task);

        assertThatThrownBy(() -> appService.delete(created.getId()))
                .isInstanceOf(ConnectorReferencedException.class)
                .hasMessageContaining("被采集任务引用");
        // 连接器未被删除
        assertThat(repository.findById(created.getId())).isPresent();
    }

    @Test
    @DisplayName("测试连接成功：状态流转为已连接并持久化")
    void testConnectionSuccessMarksConnected() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));
        connectorTestSpi.enqueue(ConnectTestResult.success("连接成功"));

        ConnectTestResult result = appService.testConnection(created.getId());

        assertThat(result.isConnected()).isTrue();
        assertThat(repository.findById(created.getId()).orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(ConnectorStatus.CONNECTED);
    }

    @Test
    @DisplayName("测试连接网络失败：分类 network、状态流转为失败并持久化")
    void testConnectionNetworkFailureClassifies() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接到主机，请检查网络"));

        assertThatThrownBy(() -> appService.testConnection(created.getId()))
                .isInstanceOf(ConnectTestException.class)
                .extracting(ex -> ((ConnectTestException) ex).getErrorType())
                .isEqualTo(ConnectErrorType.NETWORK);

        assertThat(repository.findById(created.getId()).orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(ConnectorStatus.FAILED);
    }

    @Test
    @DisplayName("测试连接凭据失败：分类 credential")
    void testConnectionCredentialFailureClassifies() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.CREDENTIAL, "用户名或密码不正确"));

        assertThatThrownBy(() -> appService.testConnection(created.getId()))
                .isInstanceOf(ConnectTestException.class)
                .extracting(ex -> ((ConnectTestException) ex).getErrorType())
                .isEqualTo(ConnectErrorType.CREDENTIAL);
    }

    @Test
    @DisplayName("测试连接方言失败：分类 dialect")
    void testConnectionDialectFailureClassifies() {
        ConnectorVO created = appService.create(buildAddCmd("订单库", "10.0.0.1", 3306, "pwd"));
        connectorTestSpi.enqueue(ConnectTestResult.failure(ConnectErrorType.DIALECT, "方言不受支持"));

        assertThatThrownBy(() -> appService.testConnection(created.getId()))
                .isInstanceOf(ConnectTestException.class)
                .extracting(ex -> ((ConnectTestException) ex).getErrorType())
                .isEqualTo(ConnectErrorType.DIALECT);
    }

    @Test
    @DisplayName("测试不存在的连接器抛出未找到（404 语义）")
    void testConnectionNotFoundThrows() {
        assertThatThrownBy(() -> appService.testConnection("not-exist"))
                .isInstanceOf(ConnectorNotFoundException.class);
    }

    private ConnectorAddCmd buildAddCmd(String name, String host, int port, String password) {
        ConnectorAddCmd cmd = new ConnectorAddCmd();
        cmd.setName(name);
        cmd.setType(ConnectorType.MYSQL);
        cmd.setHost(host);
        cmd.setPort(port);
        cmd.setDialect(Dialect.NATIVE);
        cmd.setUsername("root");
        cmd.setPassword(password);
        cmd.setAutoClassify(Boolean.TRUE);
        return cmd;
    }

    private ConnectorUpdateCmd buildUpdateCmd(String id) {
        ConnectorUpdateCmd cmd = new ConnectorUpdateCmd();
        cmd.setId(id);
        cmd.setName("订单库");
        cmd.setType(ConnectorType.MYSQL);
        cmd.setHost("10.0.0.1");
        cmd.setPort(3306);
        cmd.setDialect(Dialect.NATIVE);
        cmd.setUsername("root");
        cmd.setAutoClassify(Boolean.TRUE);
        return cmd;
    }
}
