package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.MetricCreateInput;
import com.yss.datamiddle.semantic.application.model.MetricVersionInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.metric.exception.MetricNameDuplicateException;
import com.yss.datamiddle.semantic.metric.gateway.MetricGateway;
import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MetricServiceTest {

    private final MetricGateway gateway = Mockito.mock(MetricGateway.class);
    private final CurrentUserPort userPort = new CurrentUserPort() {
        @Override
        public String userName() {
            return "test_user";
        }

        @Override
        public boolean isWritePermitted() {
            return true;
        }
    };
    private final MetricService service = new MetricService(gateway, userPort);

    @Test
    @DisplayName("新建指标口径同名抛出 MetricNameDuplicateException (422)")
    void duplicateNameThrowsException() {
        when(gateway.findByName("GMV")).thenReturn(Optional.of(new MetricDefinition()));

        MetricCreateInput input = MetricCreateInput.builder().name("GMV").build();
        assertThrows(MetricNameDuplicateException.class, () -> service.create(input));
    }

    @Test
    @DisplayName("新增版本并更新聚合")
    void addVersionSuccess() {
        MetricDefinition m = MetricDefinition.create("GMV", "g1", "desc", "owner", "u1");
        m.setId(10L);
        when(gateway.findById(10L)).thenReturn(Optional.of(m));
        when(gateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        MetricVersionInput vInput = MetricVersionInput.builder().expression("sum(amt)").build();
        MetricVersion v = service.addVersion(10L, vInput);

        assertNotNull(v);
        assertEquals(1, v.getVersionNo());
        assertEquals(1, m.getCurrentVersionNo());
    }
}
