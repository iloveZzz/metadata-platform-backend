package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.governance.model.PropagateTask;
import com.yss.metadata.domain.governance.model.PropagateTaskStatus;
import com.yss.metadata.repository.entity.PropagateTaskPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 分类传播任务持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface PropagateTaskConvertor {

    PropagateTaskPO toPO(PropagateTask task);

    PropagateTask toDomain(PropagateTaskPO po);

    List<PropagateTask> toDomainList(List<PropagateTaskPO> pos);

    default String mapStatus(PropagateTaskStatus status) {
        return status == null ? null : status.getValue();
    }

    default PropagateTaskStatus mapStatus(String value) {
        return PropagateTaskStatus.fromValue(value);
    }
}
