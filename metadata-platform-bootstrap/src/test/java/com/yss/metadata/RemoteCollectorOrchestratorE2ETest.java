package com.yss.metadata;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.datasource.ConnectionTestVO;
import com.yss.datamiddleds.client.dto.metadata.ColumnVO;
import com.yss.datamiddleds.client.dto.metadata.TableDetailVO;
import com.yss.datamiddleds.client.dto.metadata.TableSummaryVO;
import com.yss.datamiddleds.client.feign.ConnectionTestFeignClient;
import com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient;
import com.yss.metadata.application.collector.service.CollectorOrchestrator;
import com.yss.metadata.application.collector.service.convertor.CollectorAppConvertor;
import com.yss.metadata.application.governance.service.support.SensitiveRecognitionApplier;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.gateway.CollectorTaskGateway;
import com.yss.metadata.domain.collector.model.*;
import com.yss.metadata.domain.connector.gateway.ConnectorGateway;
import com.yss.metadata.domain.connector.model.Connector;
import com.yss.metadata.domain.connector.model.ConnectorStatus;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import com.yss.metadata.domain.governance.gateway.ClassRuleGateway;
import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.infrastructure.collector.RemoteMetadataCollectorSpiImpl;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
import com.yss.metadata.infrastructure.connector.RemoteConnectorTestSpiImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 远端 SPI 适配器与应用层编排器端到端集成测试（Slice 04 E2E）。
 */
@ExtendWith(MockitoExtension.class)
class RemoteCollectorOrchestratorE2ETest {

    @Mock
    private ConnectionTestFeignClient connectionTestFeignClient;

    @Mock
    private DatasourceMetadataFeignClient datasourceMetadataFeignClient;

    @Mock
    private ConnectorGateway connectorGateway;

    @Mock
    private CollectorTaskGateway collectorTaskGateway;

    @Mock
    private AssetGateway assetGateway;

    @Mock
    private ClassRuleGateway classRuleGateway;

    @Mock
    private ClassificationGateway classificationGateway;

    private CollectorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MetadataCollectorConvertor metadataConvertor = Mappers.getMapper(MetadataCollectorConvertor.class);
        RemoteConnectorTestSpiImpl connectorTestSpi = new RemoteConnectorTestSpiImpl(connectionTestFeignClient, metadataConvertor);
        RemoteMetadataCollectorSpiImpl collectorExecutionSpi = new RemoteMetadataCollectorSpiImpl(datasourceMetadataFeignClient, metadataConvertor);

        SensitiveRecognitionApplier sensitiveRecognitionApplier =
                new SensitiveRecognitionApplier(classRuleGateway, classificationGateway);
        CollectorAppConvertor collectorConvertor = Mappers.getMapper(CollectorAppConvertor.class);

        orchestrator = new CollectorOrchestrator(
                collectorTaskGateway,
                connectorGateway,
                connectorTestSpi,
                collectorExecutionSpi,
                assetGateway,
                sensitiveRecognitionApplier,
                collectorConvertor
        );
    }

    @Test
    @DisplayName("端到端闭环：连通性校验 -> 两阶段元数据分级扫描 -> 防腐转换 -> 资产幂等入库 -> 状态更新为 SUCCESS")
    void testOrchestratorFullSuccessWithRemoteSpi() {
        Connector connector = buildConnector("ds-mysql-01");
        CollectorTask task = buildTask("task-100", "ds-mysql-01");

        when(collectorTaskGateway.findById("task-100")).thenReturn(Optional.of(task));
        when(connectorGateway.findById("ds-mysql-01")).thenReturn(Optional.of(connector));

        // 1. 模拟连通性测试通过
        ConnectionTestVO testVO = new ConnectionTestVO();
        testVO.setSuccess(true);
        testVO.setMessage("连接正常");
        when(connectionTestFeignClient.testDataSourceConnection(eq("ds-mysql-01")))
                .thenReturn(SingleResult.of(testVO));

        // 2. 模拟表列表查询返回 2 张表
        TableSummaryVO t1 = new TableSummaryVO();
        t1.setTableName("t_trade_order");
        t1.setTableType("TABLE");
        TableSummaryVO t2 = new TableSummaryVO();
        t2.setTableName("t_user_profile");
        t2.setTableType("TABLE");

        when(datasourceMetadataFeignClient.listTables(eq("ds-mysql-01"), isNull(), isNull(), eq("ALL"), isNull(), eq(true)))
                .thenReturn(MultiResult.of(Arrays.asList(t1, t2)));

        // 3. 模拟单表聚合详情
        TableDetailVO d1 = buildDetail("t_trade_order", "order_id", "amount");
        TableDetailVO d2 = buildDetail("t_user_profile", "user_id", "mobile");
        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-mysql-01"), eq("t_trade_order"), any(), any(), eq(true)))
                .thenReturn(SingleResult.of(d1));
        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-mysql-01"), eq("t_user_profile"), any(), any(), eq(true)))
                .thenReturn(SingleResult.of(d2));

        when(assetGateway.saveAssets(eq("ds-mysql-01"), anyList()))
                .thenReturn(Collections.singletonList(new SavedAssetRef("asset-1", "t_trade_order", Collections.emptyList())));

        // 4. 触发编排
        CollectorVO result = orchestrator.run("task-100");

        // 5. 验证结果与入库
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getFailReason()).isNull();

        ArgumentCaptor<List<CollectedAsset>> captor = ArgumentCaptor.forClass(List.class);
        verify(assetGateway).saveAssets(eq("ds-mysql-01"), captor.capture());
        List<CollectedAsset> savedAssets = captor.getValue();
        assertThat(savedAssets).hasSize(2);
        assertThat(savedAssets.get(0).getName()).isEqualTo("t_trade_order");
        assertThat(savedAssets.get(1).getName()).isEqualTo("t_user_profile");

        verify(collectorTaskGateway).save(argThat(t -> t.getStatus() == CollectorTaskStatus.SUCCESS));
    }

    @Test
    @DisplayName("端到端阻断：连通性校验失败 -> 阻断采集执行 -> 资产不入库 -> 状态更新为 FAILED 并携带错误信息")
    void testOrchestratorConnectorFailureBlocksCollector() {
        Connector connector = buildConnector("ds-mysql-02");
        CollectorTask task = buildTask("task-101", "ds-mysql-02");

        when(collectorTaskGateway.findById("task-101")).thenReturn(Optional.of(task));
        when(connectorGateway.findById("ds-mysql-02")).thenReturn(Optional.of(connector));

        // 模拟连通性测试认证失败
        ConnectionTestVO testVO = new ConnectionTestVO();
        testVO.setSuccess(false);
        testVO.setErrorCategory("AUTH");
        testVO.setMessage("Access denied for user 'root'@'localhost'");
        when(connectionTestFeignClient.testDataSourceConnection(eq("ds-mysql-02")))
                .thenReturn(SingleResult.of(testVO));

        CollectorVO result = orchestrator.run("task-101");

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getFailReason()).contains("Access denied");

        verify(assetGateway, org.mockito.Mockito.never()).saveAssets(anyString(), anyList());
        verify(collectorTaskGateway).save(argThat(t -> t.getStatus() == CollectorTaskStatus.FAILED && t.getFailReason().contains("Access denied")));
    }

    private TableDetailVO buildDetail(String tableName, String... colNames) {
        TableSummaryVO summary = new TableSummaryVO();
        summary.setTableName(tableName);
        summary.setTableType("TABLE");

        ColumnVO[] cols = Arrays.stream(colNames).map(name -> {
            ColumnVO c = new ColumnVO();
            c.setColumnName(name);
            c.setDataType("VARCHAR");
            c.setRawType("varchar(64)");
            return c;
        }).toArray(ColumnVO[]::new);

        TableDetailVO detail = new TableDetailVO();
        detail.setTableMetadata(summary);
        detail.setColumns(Arrays.asList(cols));
        return detail;
    }

    private Connector buildConnector(String id) {
        return Connector.builder()
                .id(id)
                .name("测试数据源-" + id)
                .type(ConnectorType.MYSQL)
                .dialect(Dialect.NATIVE)
                .host("10.10.10.1")
                .port(3306)
                .username("root")
                .credentialRef("enc_token")
                .autoClassify(Boolean.TRUE)
                .status(ConnectorStatus.CONNECTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CollectorTask buildTask(String id, String connectorId) {
        return CollectorTask.builder()
                .id(id)
                .name("定时采集任务-" + id)
                .connectorId(connectorId)
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.FULL)
                .strategy(CollectorStrategy.OVERWRITE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
