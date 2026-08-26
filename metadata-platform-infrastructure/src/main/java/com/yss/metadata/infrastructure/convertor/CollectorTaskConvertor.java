package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import com.yss.metadata.repository.entity.CollectorTaskPO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 采集任务 PO ↔ Domain 转换器（MapStruct）。
 *
 * <p>枚举 ↔ 字符串、CollectSchedule ↔ 字符串；禁止手写字段映射。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface CollectorTaskConvertor {

    CollectorTaskPO toPO(CollectorTask task);

    CollectorTask toCollectorTask(CollectorTaskPO po);

    List<CollectorTask> toCollectorTaskList(List<CollectorTaskPO> pos);

    default String toScheduleValue(CollectSchedule schedule) {
        return schedule == null ? null : schedule.getValue();
    }

    default CollectSchedule toSchedule(String value) {
        return value == null ? null : new CollectSchedule(value);
    }

    default String toModeValue(CollectorMode mode) {
        return mode == null ? null : mode.getValue();
    }

    default CollectorMode toMode(String value) {
        return CollectorMode.fromValue(value);
    }

    default String toStrategyValue(CollectorStrategy strategy) {
        return strategy == null ? null : strategy.getValue();
    }

    default CollectorStrategy toStrategy(String value) {
        return CollectorStrategy.fromValue(value);
    }

    default String toStatusValue(CollectorTaskStatus status) {
        return status == null ? null : status.getValue();
    }

    default CollectorTaskStatus toStatus(String value) {
        if (value == null) {
            return null;
        }
        for (CollectorTaskStatus status : CollectorTaskStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知采集任务状态: " + value);
    }
}
