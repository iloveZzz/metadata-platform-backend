package com.yss.metadata.application.integration.service.impl;

import com.yss.metadata.application.integration.service.IntegrationService;
import com.yss.metadata.application.integration.service.convertor.IntegrationAppConvertor;
import com.yss.metadata.client.dto.cmd.IntegrationConfigCmd;
import com.yss.metadata.client.vo.IntegrationVO;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.connector.exception.ConnectTestException;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.spi.CredentialCipher;
import com.yss.metadata.domain.integration.gateway.IntegrationConfigGateway;
import com.yss.metadata.domain.integration.gateway.OpenLineageEventGateway;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;
import com.yss.metadata.domain.integration.model.IntegrationConfig;
import com.yss.metadata.domain.integration.model.OpenLineageStats;
import com.yss.metadata.domain.integration.spi.GravitinoGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 集成配置应用服务实现（WU-05-01）。
 *
 * <p>组合 VO（Gravitino/DataHub/OpenLineage 统计）；保存幂等 upsert 单例行
 * （id=1）+ 审计（integration.config）；test=true 先测 Gravitino 连接，
 * 失败抛 ConnectTestException（422 分类）不保存，成功保存 lastTest。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationServiceImpl implements IntegrationService {

    /** OpenLineage 接收端点（只读展示，自身路径） */
    public static final String RECEIVE_ENDPOINT = "/api/v1/lineage";

    /** 集成配置保存审计动作 */
    private static final String AUDIT_ACTION_CONFIG = "integration.config";

    private final IntegrationConfigGateway integrationConfigGateway;
    private final OpenLineageEventGateway openLineageEventGateway;
    private final GravitinoGateway gravitinoGateway;
    private final CredentialCipher credentialCipher;
    private final AuditLogGateway auditLogRepository;
    private final IntegrationAppConvertor integrationAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public IntegrationVO getConfig() {
        IntegrationConfig config = integrationConfigGateway.find().orElse(null);
        OpenLineageStats stats = openLineageEventGateway.stats();
        return integrationAppConvertor.toVO(config, RECEIVE_ENDPOINT, stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntegrationVO saveConfig(IntegrationConfigCmd cmd, String operator) {
        IntegrationConfig config = integrationConfigGateway.find().orElse(emptyConfig());

        // test=true：先测 Gravitino 连接（失败抛 422 分类，不保存）；成功记录 lastTest
        if (Boolean.TRUE.equals(cmd.getTest())) {
            testGravitino(cmd);
            config.setGravitinoLastTest(LocalDateTime.now().withNano(0) + " 连接测试通过");
        }
        config.setGravitinoEndpoint(trimToNull(cmd.getGravitinoEndpoint()));
        if (StringUtils.hasText(cmd.getGravitinoAuthToken())) {
            config.setGravitinoAuthRef(credentialCipher.encrypt(cmd.getGravitinoAuthToken()));
        }
        config.setGravitinoEnabled(Boolean.TRUE.equals(cmd.getGravitinoEnabled()));
        config.setDatahubEndpoint(trimToNull(cmd.getDatahubEndpoint()));
        if (StringUtils.hasText(cmd.getDatahubAuthToken())) {
            config.setDatahubAuthRef(credentialCipher.encrypt(cmd.getDatahubAuthToken()));
        }
        config.setUpdatedAt(LocalDateTime.now());

        IntegrationConfig saved = integrationConfigGateway.save(config);
        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_CONFIG)
                .object(saved.getId())
                .result("success")
                .time(LocalDateTime.now())
                .build());
        log.info("集成配置已保存，operator={}, gravitinoEnabled={}, datahubEndpoint={}",
                operator, saved.getGravitinoEnabled(), saved.getDatahubEndpoint());

        OpenLineageStats stats = openLineageEventGateway.stats();
        return integrationAppConvertor.toVO(saved, RECEIVE_ENDPOINT, stats);
    }

    private void testGravitino(IntegrationConfigCmd cmd) {
        GravitinoEndpoint endpoint = GravitinoEndpoint.builder()
                .endpoint(trimToNull(cmd.getGravitinoEndpoint()))
                .authToken(cmd.getGravitinoAuthToken())
                .build();
        ConnectTestResult result = gravitinoGateway.testConnection(endpoint);
        if (!result.isConnected()) {
            throw new ConnectTestException(result.getErrorType(), result.getMessage());
        }
    }

    private IntegrationConfig emptyConfig() {
        return IntegrationConfig.builder()
                .id(IntegrationConfig.SINGLETON_ID)
                .gravitinoEnabled(false)
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
