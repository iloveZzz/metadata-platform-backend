package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.metric.batch.MetricImportResult;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MetricBatchImportExportServiceTest {

    private MetricGateway metricGateway;
    private CurrentUserPort currentUserPort;
    private MetricBatchImportExportService service;

    @BeforeEach
    public void setUp() {
        metricGateway = Mockito.mock(MetricGateway.class);
        currentUserPort = Mockito.mock(CurrentUserPort.class);
        Mockito.when(currentUserPort.isWritePermitted()).thenReturn(true);
        Mockito.when(currentUserPort.userName()).thenReturn("admin");

        service = new MetricBatchImportExportService(metricGateway, currentUserPort);
    }

    @Test
    public void testImportNewMetricsSuccess() {
        Mockito.when(metricGateway.findByName(Mockito.anyString())).thenReturn(Optional.empty());

        String csv = "指标名称,指标分组,业务口径,负责人,计算公式,逻辑描述\n" +
                "日订单量,交易域,每日支付订单总数,owner1,count(order_id),剔除退款订单";

        MetricImportResult result = service.importFromCsv(csv, false);

        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertEquals(1, result.getSuccessCount());
        Assertions.assertEquals(0, result.getFailureCount());
        Mockito.verify(metricGateway, Mockito.times(1)).save(Mockito.any(MetricDefinition.class));
    }

    @Test
    public void testImportConflictWithoutOverwriteFails() {
        MetricDefinition existing = MetricDefinition.create("日订单量", "交易域", "旧描述", "owner1", "admin");
        Mockito.when(metricGateway.findByName("日订单量")).thenReturn(Optional.of(existing));

        String csv = "指标名称,指标分组,业务口径,负责人,计算公式,逻辑描述\n" +
                "日订单量,交易域,新描述,owner1,count(order_id),剔除退款订单";

        MetricImportResult result = service.importFromCsv(csv, false);

        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertEquals(0, result.getSuccessCount());
        Assertions.assertEquals(1, result.getFailureCount());
        Assertions.assertEquals("METRIC_ALREADY_EXISTS", result.getErrors().get(0).getErrorCode());
    }

    @Test
    public void testExportCsv() {
        MetricDefinition m = MetricDefinition.create("净利润", "财务域", "营业收入减成本", "cfo", "admin");
        Mockito.when(metricGateway.listAll()).thenReturn(Collections.singletonList(m));

        String csv = service.exportToCsv();
        Assertions.assertTrue(csv.contains("指标名称,指标分组"));
        Assertions.assertTrue(csv.contains("净利润,财务域"));
    }

    @Test
    public void testReadOnlyUserThrowsPermissionDenied() {
        Mockito.when(currentUserPort.isWritePermitted()).thenReturn(false);

        Assertions.assertThrows(PermissionDeniedException.class, () -> {
            service.importFromCsv("指标名称,指标分组\n测试,默认", false);
        });
    }
}
