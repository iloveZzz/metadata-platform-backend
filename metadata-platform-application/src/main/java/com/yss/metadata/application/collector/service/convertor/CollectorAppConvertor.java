package com.yss.metadata.application.collector.service.convertor;

import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采集任务对象转换器（MapStruct）。
 *
 * <p>Cmd → CollectorTask（生命周期字段由用例补充）、CollectorTask → VO；
 * 禁止 BeanUtils.copyProperties 或手写字段映射。</p>
 */
@Mapper(config = MapStructAppConfig.class)
public interface CollectorAppConvertor {

    /**
     * 新增命令 → 采集任务标量字段（id/状态/时间戳/失败原因由用例补充）。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failReason", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CollectorTask toCollectorTask(CollectorAddCmd cmd);

    /**
     * 采集任务 → 视图对象。
     */
    CollectorVO toVO(CollectorTask task);

    /**
     * 采集任务列表 → 视图对象列表。
     */
    List<CollectorVO> toVOList(List<CollectorTask> tasks);

    default CollectSchedule toSchedule(String schedule) {
        return schedule == null ? null : new CollectSchedule(schedule);
    }

    default String toScheduleValue(CollectSchedule schedule) {
        return schedule == null ? null : schedule.getValue();
    }

    default String toModeValue(CollectorMode mode) {
        return mode == null ? null : mode.getValue();
    }

    default String toStrategyValue(CollectorStrategy strategy) {
        return strategy == null ? null : strategy.getValue();
    }

    default String toStatusValue(CollectorTaskStatus status) {
        return status == null ? null : status.getValue();
    }

    default String toDateTimeString(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
