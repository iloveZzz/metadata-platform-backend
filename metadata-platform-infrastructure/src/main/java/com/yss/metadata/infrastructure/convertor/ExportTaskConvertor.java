package com.yss.metadata.infrastructure.convertor;

import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.repository.entity.ExportTaskPO;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 导出任务持久化转换器（MapStruct；Domain ↔ PO）。
 */
@Mapper(config = MapStructInfraConfig.class)
public interface ExportTaskConvertor {

    ExportTaskPO toPO(ExportTask task);

    ExportTask toDomain(ExportTaskPO po);

    List<ExportTask> toDomainList(List<ExportTaskPO> pos);

    default String mapStatus(ExportTaskStatus status) {
        return status == null ? null : status.getValue();
    }

    default ExportTaskStatus mapStatus(String value) {
        return ExportTaskStatus.fromValue(value);
    }
}
