package com.yss.metadata.domain.connector.gateway;

import com.yss.metadata.domain.connector.model.Connector;

import java.util.List;
import java.util.Optional;

/**
 * 连接器仓储端口（Domain 定义，Infrastructure 实现）。
 */
public interface ConnectorGateway {

    /**
     * 查询全部连接器。
     */
    List<Connector> findAll();

    /**
     * 按 id 查询连接器。
     */
    Optional<Connector> findById(String id);

    /**
     * 名称是否存在（新增时的唯一性校验）。
     */
    boolean existsByName(String name);

    /**
     * 排除指定 id 后名称是否存在（更新时避免自身冲突）。
     */
    boolean existsByNameExcluding(String name, String excludeId);

    /**
     * 保存连接器（新增或更新）。
     */
    Connector save(Connector connector);

    /**
     * 按 id 删除连接器。
     */
    void deleteById(String id);

    /**
     * 获取数据源管理服务应用系统名录。
     */
    List<com.yss.metadata.client.vo.DataSourceSystemVO> getSystemCatalog();

    /**
     * 获取指定数据源下的 Database / Catalog 列表（通过数据源元数据客户端获取）。
     */
    List<String> listDatabases(String id);
}
