package com.yss.metadata.infrastructure.collector;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.metadata.*;
import com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient;
import com.yss.metadata.domain.collector.model.*;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * RemoteMetadataCollectorSpiImpl 原型与行为验证测试。
 */
@ExtendWith(MockitoExtension.class)
class RemoteMetadataCollectorSpiImplTest {

    @Mock
    private DatasourceMetadataFeignClient datasourceMetadataFeignClient;

    private MetadataCollectorConvertor convertor;
    private RemoteMetadataCollectorSpiImpl collectorSpi;

    @BeforeEach
    void setUp() {
        convertor = Mappers.getMapper(MetadataCollectorConvertor.class);
        collectorSpi = new RemoteMetadataCollectorSpiImpl(datasourceMetadataFeignClient, convertor);
    }

    @Test
    @DisplayName("采集全流程成功：目录探测 -> 表全量详情拉取 -> 组装资产列表成功")
    void execute_Success_FullWorkflow() {
        CollectorTask task = buildTask("ds-100");

        TableSummaryVO t1 = new TableSummaryVO();
        t1.setTableName("t_user");
        t1.setTableType("TABLE");

        TableSummaryVO t2 = new TableSummaryVO();
        t2.setTableName("t_order");
        t2.setTableType("TABLE");

        MultiResult<TableSummaryVO> tablesResult = MultiResult.of(Arrays.asList(t1, t2));
        when(datasourceMetadataFeignClient.listTables(eq("ds-100"), isNull(), isNull(), eq("ALL"), isNull(), eq(true)))
                .thenReturn(tablesResult);

        TableDetailVO d1 = buildDetail("t_user", "id", "name");
        TableDetailVO d2 = buildDetail("t_order", "order_id", "amount");

        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-100"), eq("t_user"), any(), any(), eq(true)))
                .thenReturn(SingleResult.of(d1));
        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-100"), eq("t_order"), any(), any(), eq(true)))
                .thenReturn(SingleResult.of(d2));

        CollectorExecutionResult result = collectorSpi.execute(task);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAssets()).hasSize(2);
        assertThat(result.getAssets().get(0).getName()).isEqualTo("t_user");
        assertThat(result.getAssets().get(1).getName()).isEqualTo("t_order");
    }

    @Test
    @DisplayName("表摘要查询失败：返回失败结果与原因")
    void execute_TableSummaryFailure_ReturnsFailure() {
        CollectorTask task = buildTask("ds-100");

        MultiResult<TableSummaryVO> failureResult = MultiResult.buildFailure("err.ds.auth", "数据库密码错误");
        when(datasourceMetadataFeignClient.listTables(anyString(), any(), any(), anyString(), any(), eq(true)))
                .thenReturn(failureResult);

        CollectorExecutionResult result = collectorSpi.execute(task);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).contains("数据库密码错误");
    }

    @Test
    @DisplayName("单表采集异常容错隔离：部分表失败不阻断批次，成功收集其余表")
    void execute_PartialTableFailure_FaultTolerantIsolation() {
        CollectorTask task = buildTask("ds-100");

        TableSummaryVO t1 = new TableSummaryVO();
        t1.setTableName("t_user");
        TableSummaryVO t2 = new TableSummaryVO();
        t2.setTableName("t_temp_dropped");

        when(datasourceMetadataFeignClient.listTables(anyString(), any(), any(), anyString(), any(), eq(true)))
                .thenReturn(MultiResult.of(Arrays.asList(t1, t2)));

        TableDetailVO d1 = buildDetail("t_user", "id");
        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-100"), eq("t_user"), any(), any(), eq(true)))
                .thenReturn(SingleResult.of(d1));
        when(datasourceMetadataFeignClient.getTableDetail(eq("ds-100"), eq("t_temp_dropped"), any(), any(), eq(true)))
                .thenThrow(new RuntimeException("Table was dropped concurrently"));

        CollectorExecutionResult result = collectorSpi.execute(task);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAssets()).hasSize(1);
        assertThat(result.getAssets().get(0).getName()).isEqualTo("t_user");
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

    private CollectorTask buildTask(String connectorId) {
        return CollectorTask.builder()
                .id("ct-101")
                .name("数据源元数据采集")
                .connectorId(connectorId)
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.FULL)
                .strategy(CollectorStrategy.OVERWRITE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.RUNNING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
