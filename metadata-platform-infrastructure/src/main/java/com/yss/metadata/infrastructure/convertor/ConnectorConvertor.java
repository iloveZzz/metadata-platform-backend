package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import com.yss.metadata.repository.entity.ConnectorPO;
import com.yss.datamiddleds.client.dto.datasource.DataSourceDetailVO;
import com.yss.datamiddleds.client.dto.datasource.DataSourceVO;
import com.yss.datamiddleds.client.dto.datasource.MaskedConnectionInfoVO;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 连接器 PO / 远端 DataSourceVO ↔ Domain 转换器（MapStruct）。
 *
 * <p>枚举 ↔ 字符串（列存储值，与冻结 OpenAPI 枚举 value 一致）；禁止手写字段映射。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ConnectorConvertor {

    ConnectorPO toPO(Connector connector);

    Connector toConnector(ConnectorPO po);

    List<Connector> toConnectorList(List<ConnectorPO> pos);

    default Connector fromDataSourceVO(DataSourceVO vo) {
        if (vo == null) {
            return null;
        }
        ConnectorType type = toType(vo.getTypeCode());
        ConnectorStatus status = toStatus(vo.getConnectivityStatus());
        String host = vo.getFolderPath() != null && !vo.getFolderPath().trim().isEmpty()
                ? vo.getFolderPath().trim()
                : (vo.getSystemName() != null && !vo.getSystemName().trim().isEmpty()
                    ? vo.getSystemName().trim()
                    : "127.0.0.1");
        return Connector.builder()
                .id(vo.getId())
                .name(vo.getName())
                .type(type)
                .host(host)
                .port(resolveDefaultPort(type))
                .dialect(Dialect.AUTO)
                .username(vo.getOwner() != null ? vo.getOwner() : "root")
                .credentialRef("remote_managed")
                .autoClassify(Boolean.TRUE)
                .status(status)
                .createdAt(vo.getUpdatedAt() != null ? vo.getUpdatedAt() : LocalDateTime.now())
                .updatedAt(vo.getUpdatedAt() != null ? vo.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    default Connector fromDataSourceDetailVO(DataSourceDetailVO detailVO) {
        if (detailVO == null || detailVO.getDataSource() == null) {
            return null;
        }
        DataSourceVO vo = detailVO.getDataSource();
        Connector connector = fromDataSourceVO(vo);
        if (detailVO.getConnection() != null) {
            MaskedConnectionInfoVO conn = detailVO.getConnection();
            if (conn.getHost() != null && !conn.getHost().trim().isEmpty()) {
                connector.setHost(conn.getHost().trim());
            }
            if (conn.getPort() != null && conn.getPort() > 0) {
                connector.setPort(conn.getPort());
            }
            if (conn.getUsername() != null && !conn.getUsername().trim().isEmpty()) {
                connector.setUsername(conn.getUsername().trim());
            }
        }
        return connector;
    }

    default String toTypeValue(ConnectorType type) {
        return type == null ? null : type.getValue();
    }

    default ConnectorType toType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ConnectorType.MYSQL;
        }
        String clean = value.trim();
        for (ConnectorType t : ConnectorType.values()) {
            if (t.getValue().equalsIgnoreCase(clean) || t.name().equalsIgnoreCase(clean)) {
                return t;
            }
        }
        if (clean.toLowerCase().contains("mysql")) return ConnectorType.MYSQL;
        if (clean.toLowerCase().contains("oracle")) return ConnectorType.ORACLE;
        if (clean.toLowerCase().contains("oceanbase") || clean.toLowerCase().contains("ob")) return ConnectorType.OCEANBASE;
        if (clean.toLowerCase().contains("gauss")) return ConnectorType.GAUSSDB;
        if (clean.toLowerCase().contains("doris")) return ConnectorType.DORIS;
        if (clean.toLowerCase().contains("starrocks")) return ConnectorType.STARROCKS;
        if (clean.toLowerCase().contains("iceberg")) return ConnectorType.ICEBERG;
        if (clean.toLowerCase().contains("hudi")) return ConnectorType.HUDI;
        if (clean.toLowerCase().contains("paimon")) return ConnectorType.PAIMON;
        if (clean.toLowerCase().contains("s3") || clean.toLowerCase().contains("oss")) return ConnectorType.OSS_S3;
        return ConnectorType.MYSQL;
    }

    default int resolveDefaultPort(ConnectorType type) {
        if (type == null) return 3306;
        switch (type) {
            case ORACLE: return 1521;
            case OCEANBASE: return 2881;
            case GAUSSDB: return 5432;
            case DORIS:
            case STARROCKS: return 9030;
            case MYSQL:
            default: return 3306;
        }
    }

    default String toDialectValue(Dialect dialect) {
        return dialect == null ? null : dialect.getValue();
    }

    default Dialect toDialect(String value) {
        return Dialect.fromValue(value);
    }

    default String toStatusValue(ConnectorStatus status) {
        return status == null ? null : status.getValue();
    }

    default ConnectorStatus toStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ConnectorStatus.DRAFT;
        }
        String clean = value.trim();
        for (ConnectorStatus status : ConnectorStatus.values()) {
            if (status.getValue().equalsIgnoreCase(clean) || status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        if ("SUCCESS".equalsIgnoreCase(clean)) {
            return ConnectorStatus.CONNECTED;
        }
        throw new IllegalArgumentException("未知连接器状态: " + value);
    }
}
