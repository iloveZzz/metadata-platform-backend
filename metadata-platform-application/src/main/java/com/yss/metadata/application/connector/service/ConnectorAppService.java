package com.yss.metadata.application.connector.service;

import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.dto.cmd.ConnectorUpdateCmd;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.connector.model.ConnectTestResult;

import java.util.List;

/**
 * 连接器应用服务接口（WU-01-01：CRUD + 测试连接错误分类）。
 *
 * <p>用例边界：name 唯一（409）、不存在（404）、测试连接失败分类
 * （network/credential/dialect，422）与状态持久化；凭据仅保存加密引用。</p>
 */
public interface ConnectorAppService {

    /**
     * 连接器列表。
     */
    List<ConnectorVO> list();

    /**
     * 新增连接器（初始草稿）。
     */
    ConnectorVO create(ConnectorAddCmd cmd);

    /**
     * 更新连接器配置（配置变更后状态重置草稿，需重新测试连接）。
     */
    ConnectorVO update(String id, ConnectorUpdateCmd cmd);

    /**
     * 删除连接器（不可逆，需确认）。
     */
    void delete(String id);

    /**
     * 测试连接：成功流转已连接；失败流转失败并抛出分类异常（422 语义）。
     */
    ConnectTestResult testConnection(String id);

    /**
     * 获取按数据源类型聚合的统计信息（已创建实例数、已采集资产数）。
     */
    List<com.yss.metadata.client.vo.ConnectorTypeStatsVO> getTypeStats();

    /**
     * 获取数据源服务业务系统名录（用于采集任务来源系统选择）。
     */
    List<com.yss.metadata.client.vo.DataSourceSystemVO> getSystemCatalog();

    /**
     * 获取指定数据源下的 Database / Catalog 列表（通过数据源管理元数据 Client 获取）。
     */
    List<String> listDatabases(String id);
}
