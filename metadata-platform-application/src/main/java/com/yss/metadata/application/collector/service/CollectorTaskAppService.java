package com.yss.metadata.application.collector.service;

import com.yss.metadata.client.dto.cmd.CollectorAddCmd;
import com.yss.metadata.client.dto.cmd.CollectorUpdateCmd;
import com.yss.metadata.client.vo.CollectorVO;

import java.util.List;

/**
 * 采集任务应用服务接口（WU-01-02/03）。
 *
 * <p>核心规则在 Domain 聚合（运行中幂等、取消仅运行中、配置变更重置待执行）；
 * 用例负责创建幂等唯一（同数据源 + 调度，409）、加载/保存与事务边界。
 * 边界统一返回 {@link CollectorVO}，不透出领域聚合。</p>
 */
public interface CollectorTaskAppService {

    /**
     * 采集任务全量列表。
     */
    List<CollectorVO> list();

    /**
     * 采集任务条件过滤列表。
     */
    List<CollectorVO> list(com.yss.metadata.client.dto.query.CollectorQuery query);

    /**
     * 切换采集任务生效状态（启用/停用）。
     */
    CollectorVO toggleStatus(String id, Boolean enabled);

    /**
     * 创建采集任务（初始待执行；同数据源 + 调度唯一，冲突抛 409）。
     */
    CollectorVO create(CollectorAddCmd cmd);

    /**
     * 编辑采集任务调度（配置变更后状态重置待执行；同数据源 + 调度唯一排除自身）。
     */
    CollectorVO update(String id, CollectorUpdateCmd cmd);

    /**
     * 开始执行（非运行中均可执行；运行中重复触发抛状态冲突，幂等拒绝）。
     */
    CollectorVO start(String id);

    /**
     * 取消（仅运行中；其余状态抛状态冲突，409 语义）。
     */
    CollectorVO cancel(String id);

    /**
     * 标记成功（仅运行中）。
     */
    CollectorVO markSucceeded(String id);

    /**
     * 根据 ID 获取采集任务详情（不存在抛 404）。
     */
    CollectorVO getById(String id);

    /**
     * 删除采集任务（运行中删除抛 409 状态冲突；同时清除调度）。
     */
    void delete(String id);

    /**
     * 标记失败（仅运行中），持久化失败原因（局部重采语义字段）。
     */
    CollectorVO markFailed(String id, String cause);
}
