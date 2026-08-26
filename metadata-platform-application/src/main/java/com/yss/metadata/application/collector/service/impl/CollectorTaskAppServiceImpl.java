package com.yss.metadata.application.collector.service.impl;

import com.yss.metadata.application.collector.service.CollectorTaskAppService;
import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.dto.cmd.CollectorUpdateCmd;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.domain.collector.gateway.CollectorSchedulerGateway;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import com.yss.metadata.domain.collector.exception.CollectorTaskConflictException;
import com.yss.metadata.domain.collector.exception.CollectorTaskNotFoundException;
import com.yss.metadata.domain.collector.exception.CollectorTaskStateConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 采集任务应用服务实现（WU-01-02/03）。
 *
 * <p>核心规则在 Domain 聚合（运行中幂等、取消仅运行中、配置变更重置待执行）；
 * 用例负责创建幂等唯一（同数据源 + 调度，409）、加载/保存与事务边界；
 * 持久化由 Infrastructure MyBatis-Plus Gateway 实现。
 * 边界统一返回 {@link CollectorVO}，不透出领域聚合。</p>
 */
@Service
@Slf4j
public class CollectorTaskAppServiceImpl implements CollectorTaskAppService {

    private final CollectorTaskGateway collectorTaskGateway;
    private final CollectorSchedulerGateway collectorSchedulerGateway;
    private final CollectorAppConvertor collectorAppConvertor;

    @Autowired
    public CollectorTaskAppServiceImpl(CollectorTaskGateway collectorTaskGateway,
                                      @Autowired(required = false) CollectorSchedulerGateway collectorSchedulerGateway,
                                      CollectorAppConvertor collectorAppConvertor) {
        this.collectorTaskGateway = collectorTaskGateway;
        this.collectorSchedulerGateway = collectorSchedulerGateway;
        this.collectorAppConvertor = collectorAppConvertor;
    }

    public CollectorTaskAppServiceImpl(CollectorTaskGateway collectorTaskGateway, CollectorAppConvertor collectorAppConvertor) {
        this(collectorTaskGateway, null, collectorAppConvertor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectorVO> list() {
        return collectorAppConvertor.toVOList(collectorTaskGateway.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectorVO> list(com.yss.metadata.client.dto.query.CollectorQuery query) {
        return collectorAppConvertor.toVOList(collectorTaskGateway.findByQuery(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO toggleStatus(String id, Boolean enabled) {
        CollectorTask task = requireById(id);
        task.toggleEnabled(enabled);
        collectorTaskGateway.save(task);
        if (collectorSchedulerGateway != null) {
            collectorSchedulerGateway.syncSchedule(task);
        }
        log.info("切换采集任务生效状态成功，id={}, enabled={}", id, task.getEnabled());
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO create(CollectorAddCmd cmd) {
        if (collectorTaskGateway.existsByConnectorIdAndSchedule(cmd.getConnectorId(),
                new CollectSchedule(cmd.getSchedule()))) {
            throw new CollectorTaskConflictException(cmd.getConnectorId(), cmd.getSchedule());
        }
        CollectorTask task = collectorAppConvertor.toCollectorTask(cmd);
        task.setId(UUID.randomUUID().toString());
        task.setStatus(CollectorTaskStatus.PENDING);
        if (task.getEnabled() == null) {
            task.setEnabled(Boolean.TRUE);
        }
        if (task.getOwner() == null || task.getOwner().trim().isEmpty()) {
            task.setOwner("1397905662202719");
        }
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.validate();
        collectorTaskGateway.save(task);
        if (collectorSchedulerGateway != null) {
            collectorSchedulerGateway.syncSchedule(task);
        }
        log.info("创建采集任务成功，id={}, name={}, connectorId={}", task.getId(), task.getName(), task.getConnectorId());
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO update(String id, CollectorUpdateCmd cmd) {
        CollectorTask task = requireById(id);
        if (collectorTaskGateway.existsByConnectorIdAndScheduleExcluding(cmd.getConnectorId(),
                new CollectSchedule(cmd.getSchedule()), id)) {
            throw new CollectorTaskConflictException(cmd.getConnectorId(), cmd.getSchedule());
        }
        task.updateDetails(cmd.getName(), cmd.getConnectorId(), new CollectSchedule(cmd.getSchedule()),
                cmd.getMode(), cmd.getStrategy(), cmd.getAutoClassify(), cmd.getOwner(), cmd.getDescription(),
                cmd.getEnabled(), cmd.getDatasourceType(), cmd.getSourceSystem(), cmd.getScopeType(),
                cmd.getSelectedDatabases(), cmd.getRetryEnabled(), cmd.getRetryCount(), cmd.getRetryInterval());
        collectorTaskGateway.save(task);
        if (collectorSchedulerGateway != null) {
            collectorSchedulerGateway.syncSchedule(task);
        }
        log.info("更新采集任务成功，id={}, name={}", task.getId(), task.getName());
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO start(String id) {
        CollectorTask task = requireById(id);
        task.start();
        collectorTaskGateway.save(task);
        log.info("采集任务开始执行，id={}", id);
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO cancel(String id) {
        CollectorTask task = requireById(id);
        task.cancel();
        collectorTaskGateway.save(task);
        log.info("采集任务已取消，id={}", id);
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public CollectorVO getById(String id) {
        CollectorTask task = requireById(id);
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        CollectorTask task = requireById(id);
        if (task.getStatus() == CollectorTaskStatus.RUNNING) {
            throw new CollectorTaskStateConflictException("运行中的采集任务不能删除，请先停止任务");
        }
        if (collectorSchedulerGateway != null) {
            collectorSchedulerGateway.cancelSchedule(id);
        }
        collectorTaskGateway.deleteById(id);
        log.info("删除采集任务成功，id={}, name={}", id, task.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO markSucceeded(String id) {
        CollectorTask task = requireById(id);
        task.markSucceeded();
        collectorTaskGateway.save(task);
        log.info("采集任务执行成功，id={}", id);
        return collectorAppConvertor.toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorVO markFailed(String id, String cause) {
        CollectorTask task = requireById(id);
        task.markFailed(cause);
        collectorTaskGateway.save(task);
        log.info("采集任务执行失败，id={}, cause={}", id, cause);
        return collectorAppConvertor.toVO(task);
    }

    private CollectorTask requireById(String id) {
        return collectorTaskGateway.findById(id)
                .orElseThrow(() -> new CollectorTaskNotFoundException(id));
    }
}
