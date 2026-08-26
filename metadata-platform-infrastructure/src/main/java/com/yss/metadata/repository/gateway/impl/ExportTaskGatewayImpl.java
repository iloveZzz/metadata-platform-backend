package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.lineage.gateway.ExportTaskGateway;
import com.yss.metadata.domain.lineage.model.ExportTask;
import com.yss.metadata.domain.lineage.model.ExportTaskStatus;
import com.yss.metadata.repository.ExportTaskRepository;
import com.yss.metadata.infrastructure.convertor.ExportTaskConvertor;
import com.yss.metadata.repository.entity.ExportTaskPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 导出任务仓储实现（MyBatis-Plus；export_task 表）。
 *
 * <p>幂等复用判定：同 asset_id+format 进行中任务（pending/running）优先返回；
 * 状态流转经 save 持久化。</p>
 */
@Repository
public class ExportTaskGatewayImpl implements ExportTaskGateway {

    private final ExportTaskRepository exportTaskRepository;
    private final ExportTaskConvertor exportTaskConvertor;

    @Autowired
    public ExportTaskGatewayImpl(ExportTaskRepository exportTaskRepository) {
        this(exportTaskRepository, Mappers.getMapper(ExportTaskConvertor.class));
    }

    public ExportTaskGatewayImpl(ExportTaskRepository exportTaskRepository, ExportTaskConvertor exportTaskConvertor) {
        this.exportTaskRepository = exportTaskRepository;
        this.exportTaskConvertor = exportTaskConvertor != null ? exportTaskConvertor : Mappers.getMapper(ExportTaskConvertor.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExportTask> findInProgress(String assetId, String format) {
        List<String> inProgress = Arrays.asList(
                ExportTaskStatus.PENDING.getValue(), ExportTaskStatus.RUNNING.getValue());
        // 切片 05：assetId 为空 = 全局导出（DataHub，asset_id IS NULL）
        ExportTaskPO po = exportTaskRepository.selectOne(Wrappers.<ExportTaskPO>lambdaQuery()
                .eq(assetId != null, ExportTaskPO::getAssetId, assetId)
                .isNull(assetId == null, ExportTaskPO::getAssetId)
                .eq(ExportTaskPO::getFormat, format)
                .in(ExportTaskPO::getStatus, inProgress)
                .last("LIMIT 1"));
        return po == null ? Optional.empty() : Optional.of(exportTaskConvertor.toDomain(po));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTask save(ExportTask task) {
        ExportTaskPO po = exportTaskConvertor.toPO(task);
        if (exportTaskRepository.selectById(po.getId()) != null) {
            exportTaskRepository.updateById(po);
        } else {
            exportTaskRepository.insert(po);
        }
        return exportTaskConvertor.toDomain(po);
    }
}
