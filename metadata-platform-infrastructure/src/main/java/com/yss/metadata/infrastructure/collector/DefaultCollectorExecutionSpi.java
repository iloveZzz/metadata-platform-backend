package com.yss.metadata.infrastructure.collector;

import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.spi.CollectorExecutionSpi;
import org.springframework.stereotype.Component;

/**
 * 默认采集执行适配器（seam-deferred）。
 *
 * <p>物理采集执行（连接数据源读取元数据 → 资产/版本入库）尚未实现：
 * Gravitino 采集上游随切片 05 集成；本实现明确返回失败提示，不伪装成功。</p>
 */
@Component
public class DefaultCollectorExecutionSpi implements CollectorExecutionSpi {

    @Override
    public CollectorExecutionResult execute(CollectorTask task) {
        return CollectorExecutionResult.failure(
                "采集执行物理接入尚未实现（seam-deferred，Gravitino 上游随切片 05 集成）");
    }
}
