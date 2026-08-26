package com.yss.metadata.infrastructure.connector;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.datasource.CallerPermissionsVO;
import com.yss.datamiddleds.client.dto.datasource.DataSourceDetailVO;
import com.yss.datamiddleds.client.dto.datasource.DataSourceVO;
import com.yss.datamiddleds.client.dto.datasource.MaskedConnectionInfoVO;
import com.yss.datamiddleds.client.feign.DataSourceFeignClient;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.infrastructure.convertor.ConnectorConvertor;
import com.yss.metadata.repository.ConnectorRepository;
import com.yss.metadata.repository.entity.ConnectorPO;
import com.yss.metadata.repository.gateway.impl.ConnectorGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ConnectorGatewayImpl 通过 DataSourceFeignClient 从统一数据源微服务获取数据源及回退行为。
 */
@ExtendWith(MockitoExtension.class)
class RemoteDataSourceConnectorGatewayTest {

    @Mock
    private ConnectorRepository connectorRepository;

    @Mock
    private DataSourceFeignClient dataSourceFeignClient;

    private ConnectorGateway connectorGateway;

    @BeforeEach
    void setUp() {
        ConnectorConvertor convertor = Mappers.getMapper(ConnectorConvertor.class);
        connectorGateway = new ConnectorGatewayImpl(connectorRepository, dataSourceFeignClient, convertor);
    }

    @Test
    @DisplayName("findAll 优先从 DataSourceFeignClient 获取远端数据源列表并正确转换")
    void testFindAll_FromRemoteFeignClient() {
        DataSourceVO ds1 = DataSourceVO.builder()
                .id("ds-mysql-01")
                .name("核心交易主库")
                .typeCode("MySQL")
                .folderPath("销售域 / 订单")
                .owner("1397905662202719")
                .environment("PROD")
                .connectivityStatus("SUCCESS")
                .updatedAt(LocalDateTime.now())
                .permissions(CallerPermissionsVO.builder().canEdit(true).canViewConnection(true).canTestConnection(true).build())
                .build();

        DataSourceVO ds2 = DataSourceVO.builder()
                .id("ds-oracle-01")
                .name("ERP财务中心库")
                .typeCode("Oracle")
                .folderPath("财务域 / 账务")
                .owner("admin")
                .environment("PROD")
                .connectivityStatus("SUCCESS")
                .updatedAt(LocalDateTime.now())
                .build();

        when(dataSourceFeignClient.pageDataSources(any())).thenReturn(PageResult.of(Arrays.asList(ds1, ds2), 2L, 1, 20));

        List<Connector> connectors = connectorGateway.findAll();

        assertThat(connectors).hasSize(2);
        assertThat(connectors.get(0).getId()).isEqualTo("ds-mysql-01");
        assertThat(connectors.get(0).getName()).isEqualTo("核心交易主库");
        assertThat(connectors.get(0).getType()).isEqualTo(ConnectorType.MYSQL);
        assertThat(connectors.get(0).getStatus()).isEqualTo(ConnectorStatus.CONNECTED);
        assertThat(connectors.get(0).getHost()).isEqualTo("销售域 / 订单");

        assertThat(connectors.get(1).getId()).isEqualTo("ds-oracle-01");
        assertThat(connectors.get(1).getType()).isEqualTo(ConnectorType.ORACLE);
    }

    @Test
    @DisplayName("findById 优先从 DataSourceFeignClient 获取远端数据源详情")
    void testFindById_FromRemoteFeignClient() {
        DataSourceVO ds = DataSourceVO.builder()
                .id("ds-mysql-01")
                .name("核心交易主库")
                .typeCode("MySQL")
                .folderPath("销售域 / 订单")
                .owner("1397905662202719")
                .environment("PROD")
                .connectivityStatus("SUCCESS")
                .build();

        MaskedConnectionInfoVO conn = MaskedConnectionInfoVO.builder()
                .host("10.0.1.20")
                .port(3306)
                .username("root")
                .build();

        DataSourceDetailVO detail = DataSourceDetailVO.builder()
                .dataSource(ds)
                .connection(conn)
                .build();

        when(dataSourceFeignClient.getDataSource(eq("ds-mysql-01"))).thenReturn(SingleResult.of(detail));

        Optional<Connector> result = connectorGateway.findById("ds-mysql-01");

        assertThat(result).isPresent();
        Connector connector = result.get();
        assertThat(connector.getId()).isEqualTo("ds-mysql-01");
        assertThat(connector.getName()).isEqualTo("核心交易主库");
        assertThat(connector.getHost()).isEqualTo("10.0.1.20");
        assertThat(connector.getPort()).isEqualTo(3306);
        assertThat(connector.getUsername()).isEqualTo("root");
        assertThat(connector.getStatus()).isEqualTo(ConnectorStatus.CONNECTED);
    }

    @Test
    @DisplayName("当 DataSourceFeignClient 调用异常时，抛出 IllegalStateException 提示失败")
    void testFindAll_RemoteClientException_ThrowsIllegalStateException() {
        when(dataSourceFeignClient.pageDataSources(any())).thenThrow(new RuntimeException("Feign service unavailable"));

        assertThatThrownBy(() -> connectorGateway.findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("调用统一数据源服务获取数据源列表失败: Feign service unavailable");
    }

    @Test
    @DisplayName("当 DataSourceFeignClient 返回业务失败时，抛出 IllegalStateException 提示失败")
    void testFindAll_RemoteClientFailure_ThrowsIllegalStateException() {
        PageResult<DataSourceVO> failResult = PageResult.buildFailure("DS_TIMEOUT", "数据源微服务响应超时");
        when(dataSourceFeignClient.pageDataSources(any())).thenReturn(failResult);

        assertThatThrownBy(() -> connectorGateway.findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("从统一数据源服务获取数据源列表失败: 数据源微服务响应超时");
    }

    @Test
    @DisplayName("当 DataSourceFeignClient 获取详情返回失败时，抛出 IllegalStateException 提示失败")
    void testFindById_RemoteClientFailure_ThrowsIllegalStateException() {
        SingleResult<DataSourceDetailVO> failResult = SingleResult.buildFailure("DS_ERROR", "权限校验不通过");
        when(dataSourceFeignClient.getDataSource(eq("ds-mysql-01"))).thenReturn(failResult);

        assertThatThrownBy(() -> connectorGateway.findById("ds-mysql-01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("从统一数据源服务获取数据源详情失败: 权限校验不通过");
    }

    @Test
    @DisplayName("当未装配 Feign Client 时（单机/隔离模式），使用本地仓储")
    void testFallbackToLocalRepositoryWhenNoFeignClient() {
        ConnectorConvertor convertor = Mappers.getMapper(ConnectorConvertor.class);
        ConnectorGateway localGateway = new ConnectorGatewayImpl(connectorRepository, null, convertor);

        ConnectorPO localPo = ConnectorPO.builder()
                .id("local-c1")
                .name("本地测试库")
                .type("MySQL")
                .host("127.0.0.1")
                .port(3306)
                .dialect("native")
                .username("root")
                .credentialRef("enc_token")
                .autoClassify(true)
                .status("draft")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(connectorRepository.selectList(null)).thenReturn(Collections.singletonList(localPo));

        List<Connector> list = localGateway.findAll();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("local-c1");
        assertThat(list.get(0).getName()).isEqualTo("本地测试库");
    }

    @Test
    @DisplayName("deleteById 同时调用远端 Feign 删除与本地清理")
    void testDeleteById_CallsRemoteAndLocal() {
        when(dataSourceFeignClient.deleteDataSource("ds-mysql-01")).thenReturn(com.yss.cloud.dto.result.Result.buildSuccess());
        connectorGateway.deleteById("ds-mysql-01");

        verify(dataSourceFeignClient).deleteDataSource("ds-mysql-01");
        verify(connectorRepository).deleteById("ds-mysql-01");
    }

    @Test
    @DisplayName("getSystemCatalog 通过 AppSystemFeignClient 获取远端系统名录")
    void testGetSystemCatalog_FromAppSystemFeignClient() {
        com.yss.datamiddleds.client.feign.AppSystemFeignClient appSystemFeignClient = org.mockito.Mockito.mock(com.yss.datamiddleds.client.feign.AppSystemFeignClient.class);
        ConnectorConvertor convertor = Mappers.getMapper(ConnectorConvertor.class);
        ConnectorGateway gateway = new ConnectorGatewayImpl(connectorRepository, dataSourceFeignClient, appSystemFeignClient, convertor);

        com.yss.datamiddleds.client.dto.system.AppSystemVO sys1 = com.yss.datamiddleds.client.dto.system.AppSystemVO.builder()
                .id("sys-1")
                .name("核心交易系统")
                .code("Trading-Core")
                .businessDept("核心业务部")
                .description("核心交易中台")
                .build();
        when(appSystemFeignClient.pageAppSystems(any())).thenReturn(PageResult.of(Collections.singletonList(sys1), 1L, 1, 10));

        List<com.yss.metadata.client.vo.DataSourceSystemVO> systems = gateway.getSystemCatalog();

        assertThat(systems).hasSize(1);
        assertThat(systems.get(0).getCode()).isEqualTo("Trading-Core");
        assertThat(systems.get(0).getName()).isEqualTo("核心交易系统");
        assertThat(systems.get(0).getLabel()).isEqualTo("核心交易系统 (Trading-Core)");
    }

    @Test
    @DisplayName("getSystemCatalog 在 Feign 客户端未装配时抛出异常且不降级伪造数据")
    void testGetSystemCatalog_ThrowsWhenClientMissing() {
        assertThatThrownBy(() -> connectorGateway.getSystemCatalog())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未装配");
    }

    @Test
    @DisplayName("listDatabases 通过 DatasourceMetadataFeignClient 获取 Catalog 列表")
    void testListDatabases_FromMetadataFeignClient() {
        com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient metadataClient = org.mockito.Mockito.mock(com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient.class);
        ConnectorConvertor convertor = Mappers.getMapper(ConnectorConvertor.class);
        ConnectorGateway gateway = new ConnectorGatewayImpl(connectorRepository, dataSourceFeignClient, null, metadataClient, convertor);

        com.yss.datamiddleds.client.dto.metadata.CatalogVO cat1 = new com.yss.datamiddleds.client.dto.metadata.CatalogVO();
        cat1.setCatalogName("trade_prod_db");
        when(metadataClient.listCatalogs("ds-mysql-01", true)).thenReturn(com.yss.cloud.dto.result.MultiResult.of(Collections.singletonList(cat1)));

        List<String> dbs = gateway.listDatabases("ds-mysql-01");
        assertThat(dbs).containsExactly("trade_prod_db");
    }
}
