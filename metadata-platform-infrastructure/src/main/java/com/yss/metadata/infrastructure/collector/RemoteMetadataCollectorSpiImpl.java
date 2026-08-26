package com.yss.metadata.infrastructure.collector;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddleds.client.dto.metadata.TableDetailVO;
import com.yss.datamiddleds.client.dto.metadata.TableSummaryVO;
import com.yss.datamiddleds.client.feign.DatasourceMetadataFeignClient;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectorExecutionResult;
import com.yss.metadata.domain.collector.model.CollectorTask;
import com.yss.metadata.domain.collector.spi.CollectorExecutionSpi;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 datamiddle-ds-client 的远端数据源元数据采集执行 SPI 适配器。
 *
 * <p>实现两阶段分级扫描采集：
 * 1. 目录探测：调用 listTables(refresh=true) 穿透缓存拉取物理全量表摘要；
 * 2. 全量详情：遍历拉取 getTableDetail(refresh=true) 一次性获取字段与主外键/索引约束；
 * 3. 容错隔离：单表详情失败时不阻断批次，记录告警并继续完成其余表采集；
 * 4. 防腐转换：经 MetadataCollectorConvertor 转换为内部 CollectedAsset 资产模型。</p>
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class RemoteMetadataCollectorSpiImpl implements CollectorExecutionSpi {

    private final DatasourceMetadataFeignClient datasourceMetadataFeignClient;
    private final MetadataCollectorConvertor metadataCollectorConvertor;

    @Override
    public CollectorExecutionResult execute(CollectorTask task) {
        String datasourceId = task.getConnectorId();
        log.info("开始执行远端数据源元数据采集, collectorTaskId={}, datasourceId={}, mode={}",
                task.getId(), datasourceId, task.getMode());

        try {
            // 阶段 1：获取全量表摘要列表（强制 refresh=true 穿透缓存拉取最新数据）
            MultiResult<TableSummaryVO> tablesResult = datasourceMetadataFeignClient.listTables(
                    datasourceId, null, null, "ALL", null, true
            );

            if (tablesResult == null || !tablesResult.isSuccess()) {
                String errorMsg = tablesResult != null ? tablesResult.getMessage() : "远端元数据服务返回空响应";
                log.warn("拉取数据源表摘要失败, datasourceId={}, message={}", datasourceId, errorMsg);
                return CollectorExecutionResult.failure("拉取数据源表摘要失败: " + errorMsg);
            }

            List<TableSummaryVO> tableSummaries = tablesResult.getData();
            if (tableSummaries == null || tableSummaries.isEmpty()) {
                log.info("数据源下无可用表资产, datasourceId={}", datasourceId);
                return CollectorExecutionResult.success(Collections.emptyList());
            }

            // 阶段 2：逐表获取全量详情（表头、列明细、主键约束、索引）
            List<CollectedAsset> collectedAssets = new ArrayList<>();
            List<String> failedTables = new ArrayList<>();

            for (TableSummaryVO summary : tableSummaries) {
                try {
                    SingleResult<TableDetailVO> detailResult = datasourceMetadataFeignClient.getTableDetail(
                            datasourceId, summary.getTableName(), summary.getCatalogName(), summary.getSchemaName(), true
                    );

                    if (detailResult != null && detailResult.isSuccess() && detailResult.getData() != null) {
                        CollectedAsset asset = metadataCollectorConvertor.toCollectedAsset(detailResult.getData());
                        collectedAssets.add(asset);
                    } else {
                        String msg = detailResult != null ? detailResult.getMessage() : "详情响应为空";
                        log.warn("单表详情采集失败（容错跳过）, table={}, message={}", summary.getTableName(), msg);
                        failedTables.add(summary.getTableName() + "(" + msg + ")");
                    }
                } catch (Exception e) {
                    log.warn("单表详情采集异常（容错跳过）, table={}, error={}", summary.getTableName(), e.getMessage());
                    failedTables.add(summary.getTableName() + "(异常: " + e.getMessage() + ")");
                }
            }

            if (collectedAssets.isEmpty() && !failedTables.isEmpty()) {
                return CollectorExecutionResult.failure("所有表详情采集均失败: " + String.join(", ", failedTables));
            }

            if (!failedTables.isEmpty()) {
                log.warn("采集完成，部分表采集失败（容错隔离）: count={}, failed={}", failedTables.size(), failedTables);
            }

            log.info("远端元数据采集成功, datasourceId={}, 采集资产数={}", datasourceId, collectedAssets.size());
            return CollectorExecutionResult.success(collectedAssets);

        } catch (Exception e) {
            log.error("远端元数据采集执行异常, datasourceId={}, error={}", datasourceId, e.getMessage(), e);
            return CollectorExecutionResult.failure("数据源服务通信异常: " + e.getMessage());
        }
    }
}
