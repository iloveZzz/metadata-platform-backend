package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.datasource.DataSourceDetailVO;
import com.yss.datamiddleds.client.dto.datasource.DataSourcePageQuery;
import com.yss.datamiddleds.client.dto.datasource.DataSourceVO;
import com.yss.datamiddleds.client.dto.metadata.CatalogVO;
import com.yss.datamiddleds.client.dto.metadata.SchemaVO;
import com.yss.datamiddleds.client.dto.system.AppSystemPageQuery;
import com.yss.datamiddleds.client.dto.system.AppSystemVO;
import com.yss.datamiddleds.client.feign.AppSystemFeignClient;
import com.yss.datamiddleds.client.feign.DataSourceFeignClient;
import com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient;
import com.yss.metadata.client.vo.DataSourceSystemVO;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.repository.ConnectorRepository;
import com.yss.metadata.infrastructure.convertor.ConnectorConvertor;
import com.yss.metadata.repository.entity.ConnectorPO;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 连接器仓储网关实现（集成统一数据源服务 Feign 客户端与本地 MyBatis-Plus 存储）。
 *
 * <p>直接调用数据源管理微服务 (datamiddle-ds via DataSourceFeignClient / AppSystemFeignClient / DatasourceMetadataFeignClient) 获取数据源列表、应用名录与 Database 库表元数据。</p>
 */
@Repository
@Primary
@Slf4j
public class ConnectorGatewayImpl implements ConnectorGateway {

    private final ConnectorRepository connectorRepository;
    private final DataSourceFeignClient dataSourceFeignClient;
    private final AppSystemFeignClient appSystemFeignClient;
    private final DatasourceMetadataFeignClient datasourceMetadataFeignClient;
    private final ConnectorConvertor connectorConvertor;

    @Autowired
    public ConnectorGatewayImpl(ConnectorRepository connectorRepository,
                                @Autowired(required = false) DataSourceFeignClient dataSourceFeignClient,
                                @Autowired(required = false) AppSystemFeignClient appSystemFeignClient,
                                @Autowired(required = false) DatasourceMetadataFeignClient datasourceMetadataFeignClient) {
        this(connectorRepository, dataSourceFeignClient, appSystemFeignClient, datasourceMetadataFeignClient, Mappers.getMapper(ConnectorConvertor.class));
    }

    public ConnectorGatewayImpl(ConnectorRepository connectorRepository,
                                DataSourceFeignClient dataSourceFeignClient,
                                AppSystemFeignClient appSystemFeignClient,
                                DatasourceMetadataFeignClient datasourceMetadataFeignClient,
                                ConnectorConvertor connectorConvertor) {
        this.connectorRepository = connectorRepository;
        this.dataSourceFeignClient = dataSourceFeignClient;
        this.appSystemFeignClient = appSystemFeignClient;
        this.datasourceMetadataFeignClient = datasourceMetadataFeignClient;
        this.connectorConvertor = connectorConvertor != null ? connectorConvertor : Mappers.getMapper(ConnectorConvertor.class);
    }

    public ConnectorGatewayImpl(ConnectorRepository connectorRepository,
                                DataSourceFeignClient dataSourceFeignClient,
                                AppSystemFeignClient appSystemFeignClient,
                                ConnectorConvertor connectorConvertor) {
        this(connectorRepository, dataSourceFeignClient, appSystemFeignClient, null, connectorConvertor);
    }

    public ConnectorGatewayImpl(ConnectorRepository connectorRepository,
                                DataSourceFeignClient dataSourceFeignClient,
                                ConnectorConvertor connectorConvertor) {
        this(connectorRepository, dataSourceFeignClient, null, null, connectorConvertor);
    }

    public ConnectorGatewayImpl(ConnectorRepository connectorRepository, ConnectorConvertor connectorConvertor) {
        this(connectorRepository, null, null, null, connectorConvertor);
    }

    @Override
    public List<Connector> findAll() {
        if (dataSourceFeignClient != null) {
            DataSourcePageQuery query = new DataSourcePageQuery();
            query.setPageSize(200);
            PageResult<DataSourceVO> pageResult;
            try {
                pageResult = dataSourceFeignClient.pageDataSources(query);
            } catch (Exception e) {
                log.error("调用统一数据源服务获取数据源列表失败: {}", e.getMessage(), e);
                throw new IllegalStateException("调用统一数据源服务获取数据源列表失败: " + e.getMessage(), e);
            }
            if (pageResult == null) {
                throw new IllegalStateException("统一数据源服务返回空响应");
            }
            if (!pageResult.isSuccess()) {
                String errMsg = pageResult.getMessage();
                log.error("统一数据源服务获取数据源列表返回失败: code={}, msg={}", pageResult.getCode(), errMsg);
                throw new IllegalStateException("从统一数据源服务获取数据源列表失败: " + errMsg);
            }
            List<DataSourceVO> list = pageResult.getData();
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            List<Connector> remoteConnectors = list.stream()
                    .map(connectorConvertor::fromDataSourceVO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.info("通过 DataSourceFeignClient 获取远端数据源列表成功, count={}", remoteConnectors.size());
            return remoteConnectors;
        }
        return connectorConvertor.toConnectorList(connectorRepository.selectList(null));
    }

    @Override
    public Optional<Connector> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        if (dataSourceFeignClient != null) {
            SingleResult<DataSourceDetailVO> detailResult;
            try {
                detailResult = dataSourceFeignClient.getDataSource(id);
            } catch (Exception e) {
                log.error("调用统一数据源服务获取数据源详情失败 [id={}]: {}", id, e.getMessage(), e);
                throw new IllegalStateException("调用统一数据源服务获取数据源详情失败: " + e.getMessage(), e);
            }
            if (detailResult == null) {
                throw new IllegalStateException("统一数据源服务返回空响应 [id=" + id + "]");
            }
            if (!detailResult.isSuccess()) {
                String errMsg = detailResult.getMessage();
                if ("404".equals(String.valueOf(detailResult.getCode())) || (errMsg != null && (errMsg.contains("不存在") || errMsg.contains("not found")))) {
                    return Optional.empty();
                }
                log.error("统一数据源服务获取数据源详情返回失败 [id={}]: code={}, msg={}", id, detailResult.getCode(), errMsg);
                throw new IllegalStateException("从统一数据源服务获取数据源详情失败: " + errMsg);
            }
            if (detailResult.getData() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(connectorConvertor.fromDataSourceDetailVO(detailResult.getData()));
        }
        return Optional.ofNullable(connectorRepository.selectById(id)).map(connectorConvertor::toConnector);
    }

    @Override
    public boolean existsByName(String name) {
        if (dataSourceFeignClient != null) {
            DataSourcePageQuery query = new DataSourcePageQuery();
            query.setKeyword(name);
            query.setPageSize(20);
            PageResult<DataSourceVO> pageResult;
            try {
                pageResult = dataSourceFeignClient.pageDataSources(query);
            } catch (Exception e) {
                log.error("调用统一数据源服务检查数据源名称失败 [name={}]: {}", name, e.getMessage(), e);
                throw new IllegalStateException("调用统一数据源服务检查数据源名称失败: " + e.getMessage(), e);
            }
            if (pageResult == null) {
                throw new IllegalStateException("统一数据源服务返回空响应");
            }
            if (!pageResult.isSuccess()) {
                String errMsg = pageResult.getMessage();
                throw new IllegalStateException("统一数据源服务名称检查失败: " + errMsg);
            }
            if (pageResult.getData() != null) {
                return pageResult.getData().stream()
                        .anyMatch(d -> name.equalsIgnoreCase(d.getName()));
            }
            return false;
        }
        return connectorRepository.selectCount(nameWrapper(name)) > 0;
    }

    @Override
    public boolean existsByNameExcluding(String name, String excludeId) {
        if (dataSourceFeignClient != null) {
            DataSourcePageQuery query = new DataSourcePageQuery();
            query.setKeyword(name);
            query.setPageSize(20);
            PageResult<DataSourceVO> pageResult;
            try {
                pageResult = dataSourceFeignClient.pageDataSources(query);
            } catch (Exception e) {
                log.error("调用统一数据源服务检查数据源名称失败 [name={}, excludeId={}]: {}", name, excludeId, e.getMessage(), e);
                throw new IllegalStateException("调用统一数据源服务检查数据源名称失败: " + e.getMessage(), e);
            }
            if (pageResult == null) {
                throw new IllegalStateException("统一数据源服务返回空响应");
            }
            if (!pageResult.isSuccess()) {
                String errMsg = pageResult.getMessage();
                throw new IllegalStateException("统一数据源服务名称检查失败: " + errMsg);
            }
            if (pageResult.getData() != null) {
                return pageResult.getData().stream()
                        .anyMatch(d -> name.equalsIgnoreCase(d.getName()) && !Objects.equals(excludeId, d.getId()));
            }
            return false;
        }
        return connectorRepository.selectCount(nameWrapper(name).ne(ConnectorPO::getId, excludeId)) > 0;
    }

    @Override
    public Connector save(Connector connector) {
        ConnectorPO po = connectorConvertor.toPO(connector);
        if (connectorRepository.selectById(po.getId()) != null) {
            connectorRepository.updateById(po);
        } else {
            connectorRepository.insert(po);
        }
        return connector;
    }

    @Override
    public void deleteById(String id) {
        if (dataSourceFeignClient != null) {
            try {
                com.yss.cloud.dto.result.Result res = dataSourceFeignClient.deleteDataSource(id);
                if (res != null && !res.isSuccess()) {
                    String errMsg = res.getMessage();
                    log.error("统一数据源服务删除数据源失败 [id={}]: code={}, msg={}", id, res.getCode(), errMsg);
                    throw new IllegalStateException("统一数据源服务删除数据源失败: " + errMsg);
                }
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                log.error("调用统一数据源服务删除数据源异常 [id={}]: {}", id, e.getMessage(), e);
                throw new IllegalStateException("调用统一数据源服务删除数据源失败: " + e.getMessage(), e);
            }
        }
        connectorRepository.deleteById(id);
    }

    @Override
    public List<DataSourceSystemVO> getSystemCatalog() {
        if (appSystemFeignClient == null) {
            throw new IllegalStateException("数据源管理应用系统客户端 (AppSystemFeignClient) 未装配");
        }
        AppSystemPageQuery query = new AppSystemPageQuery();
        query.setPageSize(200);
        query.setPageIndex(1);
        PageResult<AppSystemVO> firstPage;
        try {
            firstPage = appSystemFeignClient.pageAppSystems(query);
        } catch (Exception e) {
            log.error("调用数据源管理 AppSystemFeignClient 获取系统名录失败: {}", e.getMessage(), e);
            throw new IllegalStateException("调用数据源管理系统名录接口失败: " + e.getMessage(), e);
        }
        if (firstPage == null) {
            throw new IllegalStateException("数据源管理服务返回空响应");
        }
        if (!firstPage.isSuccess()) {
            throw new IllegalStateException("数据源管理服务获取系统名录返回错误: " + firstPage.getMessage());
        }
        List<AppSystemVO> list = new java.util.ArrayList<>();
        if (firstPage.getData() != null) {
            list.addAll(firstPage.getData());
        }
        int pageIndex = 2;
        while (firstPage.getData() != null && firstPage.getData().size() >= 200 && pageIndex <= 20) {
            query.setPageIndex(pageIndex);
            try {
                PageResult<AppSystemVO> nextPage = appSystemFeignClient.pageAppSystems(query);
                if (nextPage != null && nextPage.isSuccess() && nextPage.getData() != null && !nextPage.getData().isEmpty()) {
                    list.addAll(nextPage.getData());
                    if (nextPage.getData().size() < 200) {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                log.warn("拉取第 {} 页数据源应用系统名录失败: {}", pageIndex, e.getMessage());
                break;
            }
            pageIndex++;
        }
        return list.stream()
                .map(vo -> DataSourceSystemVO.builder()
                        .code(vo.getCode() != null && !vo.getCode().trim().isEmpty() ? vo.getCode().trim() : vo.getId())
                        .name(vo.getName())
                        .label(vo.getName() + (vo.getCode() != null && !vo.getCode().trim().isEmpty() ? " (" + vo.getCode().trim() + ")" : ""))
                        .category(vo.getBusinessDept() != null ? vo.getBusinessDept() : "业务系统")
                        .description(vo.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listDatabases(String id) {
        if (datasourceMetadataFeignClient == null) {
            throw new IllegalStateException("数据源管理元数据客户端 (DatasourceMetadataFeignClient) 未装配");
        }
        try {
            MultiResult<CatalogVO> catalogs = datasourceMetadataFeignClient.listCatalogs(id, true);
            if (catalogs != null && catalogs.isSuccess() && catalogs.getData() != null && !catalogs.getData().isEmpty()) {
                return catalogs.getData().stream()
                        .map(CatalogVO::getCatalogName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
            }
            MultiResult<SchemaVO> schemas = datasourceMetadataFeignClient.listSchemas(id, null, true);
            if (schemas != null && schemas.isSuccess() && schemas.getData() != null && !schemas.getData().isEmpty()) {
                return schemas.getData().stream()
                        .map(SchemaVO::getSchemaName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
            }
            if (catalogs != null && !catalogs.isSuccess()) {
                throw new IllegalStateException("拉取数据源 Database 列表失败: " + catalogs.getMessage());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            log.error("调用数据源元数据客户端获取 Database 列表异常 [datasourceId={}]: {}", id, e.getMessage(), e);
            throw new IllegalStateException("调用数据源元数据客户端获取 Database 列表失败: " + e.getMessage(), e);
        }
    }

    private LambdaQueryWrapper<ConnectorPO> nameWrapper(String name) {
        return Wrappers.<ConnectorPO>lambdaQuery().eq(ConnectorPO::getName, name);
    }
}
