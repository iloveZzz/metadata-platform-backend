package com.yss.metadata.infrastructure.collector;

import com.yss.metadata.domain.collector.model.CollectSchedule;
import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorMode;
import com.yss.metadata.domain.collector.model.CollectorStrategy;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.model.CollectorTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认采集执行适配器行为测试（WU-01-03）。
 *
 * <p>合同 seam_deferred：物理采集执行尚未实现，必须明确提示、不伪装成功。</p>
 */
class DefaultCollectorExecutionSpiTest {

    private DefaultCollectorExecutionSpi spi;

    @BeforeEach
    void setUp() {
        spi = new DefaultCollectorExecutionSpi();
    }

    @Test
    @DisplayName("物理采集执行 seam-deferred：明确返回失败提示而非伪成功")
    void executeReturnsDeferredFailure() {
        CollectorExecutionResult result = spi.execute(buildTask());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).contains("seam-deferred");
    }

    private CollectorTask buildTask() {
        return CollectorTask.builder()
                .id("ct-1")
                .name("每日元数据采集")
                .connectorId("c-1")
                .schedule(new CollectSchedule("0 0 2 * * ?"))
                .mode(CollectorMode.INCREMENTAL)
                .strategy(CollectorStrategy.IGNORE)
                .autoClassify(Boolean.TRUE)
                .status(CollectorTaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
