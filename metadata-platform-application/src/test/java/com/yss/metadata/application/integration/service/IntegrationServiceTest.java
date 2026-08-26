package com.yss.metadata.application.integration.service;

import com.yss.metadata.application.connector.support.TestCredentialCipher;
import com.yss.metadata.application.integration.service.convertor.IntegrationAppConvertor;
import com.yss.metadata.application.integration.service.impl.IntegrationServiceImpl;
import com.yss.metadata.application.integration.support.FakeGravitinoGateway;
import com.yss.metadata.application.integration.support.InMemoryIntegrationConfigGateway;
import com.yss.metadata.application.integration.support.InMemoryOpenLineageEventGateway;
import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.client.dto.cmd.IntegrationConfigCmd;
import com.yss.metadata.client.vo.IntegrationVO;
import com.yss.metadata.domain.connector.exception.ConnectTestException;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.model.OpenLineageEventRecord;
import com.yss.metadata.domain.integration.model.OpenLineageParseStatus;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 集成配置应用服务测试（WU-05-01）。
 *
 * <p>覆盖：空配置空结构非错误、组合 VO 映射、保存 upsert 单例行 + 审计 +
 * 凭据加密引用、test=true 测试连接成功记录 lastTest / 失败分类 422 不保存。</p>
 */
class IntegrationServiceTest {

    private InMemoryIntegrationConfigGateway configGateway;
    private InMemoryOpenLineageEventGateway eventGateway;
    private FakeGravitinoGateway gravitinoGateway;
    private InMemoryAuditLogRepository auditLogRepository;
    private IntegrationService service;

    @BeforeEach
    void setUp() {
        configGateway = new InMemoryIntegrationConfigGateway();
        eventGateway = new InMemoryOpenLineageEventGateway();
        gravitinoGateway = new FakeGravitinoGateway();
        auditLogRepository = new InMemoryAuditLogRepository();
        service = new IntegrationServiceImpl(configGateway, eventGateway, gravitinoGateway,
                new TestCredentialCipher(), auditLogRepository, org.mapstruct.factory.Mappers.getMapper(IntegrationAppConvertor.class));
    }

    @Test
    @DisplayName("空配置返回空结构（200 语义）：Gravitino/DataHub 空对象 + OpenLineage 端点与 0 统计")
    void emptyConfigReturnsEmptyStructure() {
        IntegrationVO vo = service.getConfig();

        assertThat(vo.getGravitino()).isNotNull();
        assertThat(vo.getGravitino().getEndpoint()).isNull();
        assertThat(vo.getGravitino().getEnabled()).isNull();
        assertThat(vo.getDatahub()).isNotNull();
        assertThat(vo.getDatahub().getEndpoint()).isNull();
        assertThat(vo.getOpenLineage().getReceiveEndpoint()).isEqualTo("/api/v1/lineage");
        assertThat(vo.getOpenLineage().getRecent24h()).isZero();
        assertThat(vo.getOpenLineage().getParseSuccessRate()).isNull();
    }

    @Test
    @DisplayName("已配置 + 事件统计：组合 VO 完整映射（近 24h 事件数与解析成功率百分比）")
    void configWithStatsMapsToVO() {
        configGateway.seed(IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEndpoint("http://gravitino:8090")
                .gravitinoEnabled(true)
                .datahubEndpoint("http://datahub:8080")
                .build());
        eventGateway.save(record("e1", OpenLineageParseStatus.PARSED));
        eventGateway.save(record("e2", OpenLineageParseStatus.PARSED));
        eventGateway.save(record("e3", OpenLineageParseStatus.PARSE_FAILED));

        IntegrationVO vo = service.getConfig();

        assertThat(vo.getGravitino().getEndpoint()).isEqualTo("http://gravitino:8090");
        assertThat(vo.getGravitino().getEnabled()).isTrue();
        assertThat(vo.getDatahub().getEndpoint()).isEqualTo("http://datahub:8080");
        assertThat(vo.getOpenLineage().getRecent24h()).isEqualTo(3);
        assertThat(vo.getOpenLineage().getParseSuccessRate()).isEqualTo("66.7%");
    }

    @Test
    @DisplayName("仅保存（test 缺省）：upsert 单例行 + 审计 integration.config + 凭据加密引用")
    void saveWithoutTestPersistsAndAudits() {
        IntegrationConfigCmd cmd = new IntegrationConfigCmd();
        cmd.setGravitinoEndpoint(" http://gravitino:8090 ");
        cmd.setGravitinoAuthToken("grav-secret");
        cmd.setGravitinoEnabled(true);
        cmd.setDatahubEndpoint("http://datahub:8080");
        cmd.setDatahubAuthToken("dh-secret");

        IntegrationVO vo = service.saveConfig(cmd, "u-me");

        assertThat(vo.getGravitino().getEndpoint()).isEqualTo("http://gravitino:8090");
        IntegrationConfig saved = configGateway.find().get();
        assertThat(saved.getId()).isEqualTo(IntegrationConfig.SINGLETON_ID);
        assertThat(saved.getGravitinoAuthRef()).isNotEqualTo("grav-secret");
        assertThat(saved.getGravitinoAuthRef()).doesNotContain("grav-secret");
        assertThat(saved.getDatahubAuthRef()).doesNotContain("dh-secret");
        assertThat(saved.getGravitinoLastTest()).isNull();
        // 审计
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("integration.config");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getObject()).isEqualTo(IntegrationConfig.SINGLETON_ID);
    }

    @Test
    @DisplayName("test=true 且 Gravitino 连接成功：保存并记录 lastTest 连接测试通过")
    void saveWithTestSuccessRecordsLastTest() {
        gravitinoGateway.setResult(ConnectTestResult.success("connected"));

        IntegrationConfigCmd cmd = new IntegrationConfigCmd();
        cmd.setGravitinoEndpoint("http://gravitino:8090");
        cmd.setTest(true);

        IntegrationVO vo = service.saveConfig(cmd, "u-me");

        assertThat(vo.getGravitino().getLastTest()).contains("连接测试通过");
        assertThat(configGateway.find().get().getGravitinoLastTest()).contains("连接测试通过");
    }

    @Test
    @DisplayName("test=true 且 Gravitino 网络不可达：抛 ConnectTestException（network），不保存不审计")
    void saveWithTestNetworkFailureThrows422() {
        gravitinoGateway.setResult(ConnectTestResult.failure(ConnectErrorType.NETWORK, "无法连接 Gravitino"));

        IntegrationConfigCmd cmd = new IntegrationConfigCmd();
        cmd.setGravitinoEndpoint("http://gravitino:8090");
        cmd.setTest(true);

        assertThatThrownBy(() -> service.saveConfig(cmd, "u-me"))
                .isInstanceOf(ConnectTestException.class)
                .satisfies(ex -> {
                    ConnectTestException cte = (ConnectTestException) ex;
                    assertThat(cte.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);
                });
        assertThat(configGateway.find()).isEmpty();
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("test=true 且凭据错误：抛 ConnectTestException（credential）分类 422")
    void saveWithTestCredentialFailureClassified() {
        gravitinoGateway.setResult(ConnectTestResult.failure(ConnectErrorType.CREDENTIAL, "认证失败"));

        IntegrationConfigCmd cmd = new IntegrationConfigCmd();
        cmd.setGravitinoEndpoint("http://gravitino:8090");
        cmd.setGravitinoAuthToken("bad-token");
        cmd.setTest(true);

        assertThatThrownBy(() -> service.saveConfig(cmd, "u-me"))
                .isInstanceOf(ConnectTestException.class)
                .satisfies(ex -> assertThat(((ConnectTestException) ex).getErrorType())
                        .isEqualTo(ConnectErrorType.CREDENTIAL));
    }

    @Test
    @DisplayName("幂等 upsert：同 id=1 二次保存覆盖既有配置（不新增行）")
    void secondSaveUpsertsSameRow() {
        IntegrationConfigCmd first = new IntegrationConfigCmd();
        first.setGravitinoEndpoint("http://old:8090");
        service.saveConfig(first, "u-me");

        IntegrationConfigCmd second = new IntegrationConfigCmd();
        second.setGravitinoEndpoint("http://new:8090");
        service.saveConfig(second, "u-me");

        IntegrationConfig saved = configGateway.find().get();
        assertThat(saved.getId()).isEqualTo(IntegrationConfig.SINGLETON_ID);
        assertThat(saved.getGravitinoEndpoint()).isEqualTo("http://new:8090");
    }

    private OpenLineageEventRecord record(String id, OpenLineageParseStatus status) {
        return OpenLineageEventRecord.builder()
                .id(id)
                .eventType("COMPLETE")
                .runId("run-" + id)
                .jobNamespace("ns")
                .jobName("job")
                .parseStatus(status)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
