package com.yss.metadata.application.connector.support;

import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 连接器仓储内存实现（测试 seam，seam-deferred）。
 *
 * <p>仅供单元测试与本地运行使用，替代真实数据库持久化；
 * 生产持久化实现（MyBatis PO/Mapper）随 WU-01-03 落地并替换本 seam。</p>
 */
public class InMemoryConnectorRepository implements ConnectorGateway {

    private final ConcurrentMap<String, Connector> store = new ConcurrentHashMap<>();

    @Override
    public List<Connector> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Connector> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByName(String name) {
        return store.values().stream().anyMatch(connector -> connector.getName().equals(name));
    }

    @Override
    public boolean existsByNameExcluding(String name, String excludeId) {
        return store.values().stream()
                .anyMatch(connector -> !connector.getId().equals(excludeId) && connector.getName().equals(name));
    }

    @Override
    public Connector save(Connector connector) {
        store.put(connector.getId(), connector);
        return connector;
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public List<com.yss.metadata.client.vo.DataSourceSystemVO> getSystemCatalog() {
        return java.util.Arrays.asList(
            com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("core-trading").name("核心交易系统").label("核心交易系统 (Trading-Core)").category("核心交易域").build(),
            com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("marketing-crm").name("客户营销中台").label("客户营销中台 (Marketing-CRM)").category("营销域").build(),
            com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("risk-control").name("风险控制引擎").label("风险控制引擎 (Risk-Engine)").category("风控域").build(),
            com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("settlement").name("清算结算平台").label("清算结算平台 (Settlement-Hub)").category("清算域").build(),
            com.yss.metadata.client.vo.DataSourceSystemVO.builder().code("erp-finance").name("财务与企业资源").label("财务与企业资源 (ERP-Finance)").category("业财域").build()
        );
    }

    @Override
    public List<String> listDatabases(String id) {
        return java.util.Arrays.asList("db_trade_core", "db_trade_flow", "db_order_center");
    }
}
