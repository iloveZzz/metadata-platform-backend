package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.collector.model.CollectorInstance;
import com.yss.metadata.domain.collector.model.CollectorInstanceStatus;
import com.yss.metadata.domain.collector.model.ExecutionMode;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import com.yss.metadata.repository.entity.CollectorInstancePO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 采集实例 PO ↔ Domain 转换器（MapStruct）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface CollectorInstanceConvertor {

    CollectorInstancePO toPO(CollectorInstance instance);

    CollectorInstance toCollectorInstance(CollectorInstancePO po);

    List<CollectorInstance> toCollectorInstanceList(List<CollectorInstancePO> pos);

    default String toStatusCode(CollectorInstanceStatus status) {
        return status == null ? null : status.getCode();
    }

    default CollectorInstanceStatus toStatus(String code) {
        return CollectorInstanceStatus.fromCode(code);
    }

    default String toExecutionModeCode(ExecutionMode mode) {
        return mode == null ? null : mode.getCode();
    }

    default ExecutionMode toExecutionMode(String code) {
        return ExecutionMode.fromCode(code);
    }
}
