package com.yss.metadata.application.connector.service.impl;

import com.yss.metadata.application.connector.service.ConnectorAppService;
import com.yss.metadata.application.connector.service.convertor.ConnectorAppConvertor;
import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.dto.cmd.ConnectorUpdateCmd;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.connector.exception.ConnectTestException;
import com.yss.metadata.domain.connector.exception.ConnectorNameConflictException;
import com.yss.metadata.domain.connector.exception.ConnectorNotFoundException;
import com.yss.metadata.domain.connector.exception.ConnectorReferencedException;
import com.yss.metadata.domain.connector.spi.ConnectorTestSpi;
import com.yss.metadata.domain.connector.spi.CredentialCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 连接器应用服务实现（WU-01-01：CRUD + 测试连接错误分类）。
 *
 * <p>用例边界：name 唯一（409）、不存在（404）、测试连接失败分类
 * （network/credential/dialect，422）与状态持久化；凭据仅保存加密引用。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectorAppServiceImpl implements ConnectorAppService {

    private final ConnectorGateway connectorGateway;
    private final CollectorTaskGateway collectorTaskGateway;
    private final ConnectorTestSpi connectorTestSpi;
    private final CredentialCipher credentialCipher;
    private final ConnectorAppConvertor connectorAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorVO> list() {
        return connectorAppConvertor.toVOList(connectorGateway.findAll());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorVO create(ConnectorAddCmd cmd) {
        if (connectorGateway.existsByName(cmd.getName())) {
            throw new ConnectorNameConflictException(cmd.getName());
        }
        Connector connector = connectorAppConvertor.toConnector(cmd);
        connector.setId(UUID.randomUUID().toString());
        // 方言可选：缺省默认 AUTO（对齐冻结 OpenAPI；库表方言列 NOT NULL，禁止落 null）
        if (connector.getDialect() == null) {
            connector.setDialect(Dialect.AUTO);
        }
        connector.setCredentialRef(encryptPassword(cmd.getPassword()));
        connector.setStatus(ConnectorStatus.DRAFT);
        connector.setCreatedAt(LocalDateTime.now());
        connector.setUpdatedAt(LocalDateTime.now());
        connector.validate();
        connectorGateway.save(connector);
        log.info("新增连接器成功，id={}, name={}, type={}", connector.getId(), connector.getName(), connector.getType());
        return connectorAppConvertor.toVO(connector);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorVO update(String id, ConnectorUpdateCmd cmd) {
        Connector connector = requireById(id);
        if (connectorGateway.existsByNameExcluding(cmd.getName(), id)) {
            throw new ConnectorNameConflictException(cmd.getName());
        }
        String credentialRef = StringUtils.hasText(cmd.getPassword())
                ? credentialCipher.encrypt(cmd.getPassword())
                : connector.getCredentialRef();
        // 方言可选：缺省保留原值，避免更新时因未传方言静默改写（对齐冻结 OpenAPI）
        Dialect dialect = cmd.getDialect() != null ? cmd.getDialect() : connector.getDialect();
        connector.update(cmd.getName(), cmd.getType(), cmd.getHost(), cmd.getPort(), dialect,
                cmd.getUsername(), credentialRef, cmd.getAutoClassify());
        connectorGateway.save(connector);
        log.info("更新连接器成功，id={}, name={}", connector.getId(), connector.getName());
        return connectorAppConvertor.toVO(connector);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Connector connector = requireById(id);
        if (collectorTaskGateway.existsByConnectorId(id)) {
            throw new ConnectorReferencedException(id);
        }
        connectorGateway.deleteById(connector.getId());
        log.info("删除连接器成功，id={}, name={}", connector.getId(), connector.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ConnectTestException.class)
    public ConnectTestResult testConnection(String id) {
        Connector connector = requireById(id);
        ConnectTestResult result = connectorTestSpi.test(connector);
        if (result.isConnected()) {
            connector.markConnected();
        } else {
            connector.markTestFailed();
        }
        connectorGateway.save(connector);
        log.info("测试连接完成，id={}, connected={}, category={}", id, result.isConnected(),
                result.isConnected() ? null : result.getErrorType());
        if (!result.isConnected()) {
            throw new ConnectTestException(result.getErrorType(), result.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.yss.metadata.client.vo.ConnectorTypeStatsVO> getTypeStats() {
        List<Connector> all = connectorGateway.findAll();
        java.util.Map<String, Long> countMap = all.stream()
                .filter(c -> c.getType() != null)
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getType().name(), java.util.stream.Collectors.counting()));

        List<com.yss.metadata.client.vo.ConnectorTypeStatsVO> list = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Long> entry : countMap.entrySet()) {
            list.add(com.yss.metadata.client.vo.ConnectorTypeStatsVO.builder()
                    .type(entry.getKey())
                    .createdCount(entry.getValue().intValue())
                    .collectedCount(0)
                    .build());
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.yss.metadata.client.vo.DataSourceSystemVO> getSystemCatalog() {
        return connectorGateway.getSystemCatalog();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listDatabases(String id) {
        return connectorGateway.listDatabases(id);
    }

    private Connector requireById(String id) {
        return connectorGateway.findById(id)
                .orElseThrow(() -> new ConnectorNotFoundException(id));
    }

    private String encryptPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return null;
        }
        return credentialCipher.encrypt(password);
    }
}
