package com.yss.metadata.application.connector.service.convertor;

import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 连接器对象转换器（MapStruct）。
 *
 * <p>Cmd → Connector（生命周期字段由 AppService 补充）、Connector → VO；
 * 禁止 BeanUtils.copyProperties 或手写字段映射。</p>
 */
@Mapper(config = MapStructAppConfig.class)
public interface ConnectorAppConvertor {

    /**
     * 新增命令 → 连接器标量字段（id/凭据引用/状态/时间戳由用例补充）。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "credentialRef", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Connector toConnector(ConnectorAddCmd cmd);

    /**
     * 连接器 → 视图对象。
     */
    ConnectorVO toVO(Connector connector);

    /**
     * 连接器列表 → 视图对象列表。
     */
    List<ConnectorVO> toVOList(List<Connector> connectors);

    default String toTypeString(ConnectorType type) {
        return type == null ? null : type.getValue();
    }

    default String toDialectString(Dialect dialect) {
        return dialect == null ? null : dialect.getValue();
    }

    default String toStatusString(ConnectorStatus status) {
        return status == null ? null : status.getValue();
    }
}
