package com.yss.metadata.repository;

import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.infrastructure.convertor.AssetConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import com.yss.metadata.repository.gateway.impl.AssetGatewayImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资产入库网关持久化集成测试（WU-01-03，H2 内存库替代真实 MySQL）。
 *
 * <p>验证幂等 upsert（同 source_id + name 更新、列全量替换）、
 * 版本快照递增（schema_diff 列快照）与空清单短路。</p>
 */
class AssetGatewayImplH2Test extends H2MapperTestSupport {

    private AssetGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new AssetGatewayImpl(sqlSession.getMapper(AssetRepository.class),
                sqlSession.getMapper(AssetColumnRepository.class),
                sqlSession.getMapper(AssetVersionRepository.class),
                org.mapstruct.factory.Mappers.getMapper(AssetConvertor.class));
    }

    @Test
    @DisplayName("首次入库：资产插入（初始 pending）+ 列写入 + 版本 v1 + 技术属性写入")
    void firstSaveInsertsAssetColumnsAndVersion() {
        CollectedAsset asset = asset("orders", "PII");
        asset.setDescription("订单核心明细表");
        asset.setRowCount(10000L);
        asset.setStorageSize("5.20MB");
        gateway.saveAssets("c-1", Collections.singletonList(asset));

        AssetPO po = findAsset("c-1", "orders");
        assertThat(po).isNotNull();
        assertThat(po.getStatus()).isEqualTo("pending");
        assertThat(po.getClassification()).isEqualTo("PII");
        assertThat(po.getDescription()).isEqualTo("订单核心明细表");
        assertThat(po.getRowCount()).isEqualTo(10000L);
        assertThat(po.getStorageSize()).isEqualTo("5.20MB");
        assertThat(po.getVersion()).isNotNull().startsWith("V");
        assertThat(countColumns(po.getId())).isEqualTo(2);
        assertThat(latestVersion(po.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("同源同名二次入库：资产更新（列全量替换 + 物理指标刷新 + 描述安全保留）+ 版本递增 v2")
    void secondSaveUpdatesAndBumpsVersion() {
        CollectedAsset first = asset("orders", "PII");
        first.setDescription("初始描述");
        first.setRowCount(1000L);
        first.setStorageSize("1.00MB");
        gateway.saveAssets("c-1", Collections.singletonList(first));

        gateway.saveAssets("c-1", Collections.singletonList(
                CollectedAsset.builder()
                        .name("orders")
                        .type("table")
                        .description(null) // 远端为空，应保留初始描述
                        .rowCount(2000L)   // 物理指标强制刷新
                        .storageSize("2.50MB")
                        .columns(Arrays.asList(
                                column("id", "bigint", "主键", Boolean.TRUE),
                                column("amount", "decimal(18,2)", "金额", Boolean.FALSE),
                                column("total", "decimal(18,2)", "合计", Boolean.FALSE)))
                        .build()));

        // 资产仍只有一条（幂等 upsert）
        List<AssetPO> all = assetRepositorySelectBySource("c-1");
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getDescription()).isEqualTo("初始描述");
        assertThat(all.get(0).getRowCount()).isEqualTo(2000L);
        assertThat(all.get(0).getStorageSize()).isEqualTo("2.50MB");
        // 列全量替换：旧 2 列 → 新 3 列
        assertThat(countColumns(all.get(0).getId())).isEqualTo(3);
        // 版本递增
        assertThat(latestVersion(all.get(0).getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("更新走全量覆写语义：第二次未携带的字段被置空")
    void secondSaveClearsAbsentFields() {
        gateway.saveAssets("c-1", Collections.singletonList(asset("orders", "PII")));
        gateway.saveAssets("c-1", Collections.singletonList(
                CollectedAsset.builder().name("orders").type("table").build()));

        AssetPO po = findAsset("c-1", "orders");
        assertThat(po.getClassification()).isNull();
        assertThat(countColumns(po.getId())).isEqualTo(0);
    }

    @Test
    @DisplayName("空清单或 null 不产生任何写入")
    void emptyAssetsShortCircuits() {
        gateway.saveAssets("c-1", null);
        gateway.saveAssets("c-1", Collections.emptyList());

        assertThat(assetRepositorySelectBySource("c-1")).isEmpty();
        assertThat(sqlSession.getMapper(AssetVersionRepository.class)
                .selectList(null)).isEmpty();
    }

    @Test
    @DisplayName("增量采集保护：已剔除资产重新采集时，版本递增但强制保留 isExcluded=true 状态")
    void incrementalCollectionPreservesExcludedStatus() {
        // 1. 首次采集入库
        CollectedAsset initial = CollectedAsset.builder()
                .name("order_detail")
                .type("table")
                .databaseName("trade_db")
                .sourceSystem("核心交易系统")
                .collectorTaskId("task-1")
                .build();
        gateway.saveAssets("c-1", Collections.singletonList(initial));

        AssetPO po = findAsset("c-1", "order_detail");
        assertThat(po.getDatabaseName()).isEqualTo("trade_db");
        assertThat(po.getSourceSystem()).isEqualTo("核心交易系统");
        assertThat(po.getCollectorTaskId()).isEqualTo("task-1");
        assertThat(po.getIsExcluded()).isFalse();

        // 2. 人工在页面执行剔除（模拟标记 is_excluded = true）
        sqlSession.getMapper(AssetRepository.class).update(null,
                Wrappers.<AssetPO>lambdaUpdate()
                        .eq(AssetPO::getId, po.getId())
                        .set(AssetPO::getIsExcluded, true));

        // 3. 再次执行增量/周期性采集
        CollectedAsset updated = CollectedAsset.builder()
                .name("order_detail")
                .type("table")
                .databaseName("trade_db")
                .sourceSystem("核心交易系统")
                .collectorTaskId("task-1")
                .columns(Collections.singletonList(column("id", "bigint", "主键", true)))
                .build();
        gateway.saveAssets("c-1", Collections.singletonList(updated));

        // 4. 验证：版本更新，但 isExcluded 依旧为 true
        AssetPO refreshed = findAsset("c-1", "order_detail");
        assertThat(refreshed.getIsExcluded()).isTrue();
        assertThat(latestVersion(refreshed.getId())).isEqualTo(2);
    }

    private AssetPO findAsset(String sourceId, String name) {
        return sqlSession.getMapper(AssetRepository.class).selectOne(
                Wrappers.<AssetPO>lambdaQuery()
                        .eq(AssetPO::getSourceId, sourceId)
                        .eq(AssetPO::getName, name));
    }

    private List<AssetPO> assetRepositorySelectBySource(String sourceId) {
        return sqlSession.getMapper(AssetRepository.class).selectList(
                Wrappers.<AssetPO>lambdaQuery().eq(AssetPO::getSourceId, sourceId));
    }

    private long countColumns(String assetId) {
        return sqlSession.getMapper(AssetColumnRepository.class).selectCount(
                Wrappers.<AssetColumnPO>lambdaQuery().eq(AssetColumnPO::getAssetId, assetId));
    }

    private int latestVersion(String assetId) {
        AssetVersionPO latest = sqlSession.getMapper(AssetVersionRepository.class).selectOne(
                Wrappers.<AssetVersionPO>lambdaQuery()
                        .eq(AssetVersionPO::getAssetId, assetId)
                        .orderByDesc(AssetVersionPO::getVersion)
                        .last("LIMIT 1"));
        return latest == null ? 0 : latest.getVersion();
    }

    private CollectedAsset asset(String name, String classification) {
        return CollectedAsset.builder()
                .name(name)
                .type("table")
                .classification(classification)
                .columns(Arrays.asList(
                        column("id", "bigint", "主键", Boolean.TRUE),
                        column("amount", "decimal(18,2)", "金额", Boolean.FALSE)))
                .build();
    }

    private CollectedColumn column(String name, String type, String comment, Boolean pk) {
        return CollectedColumn.builder()
                .name(name).type(type).comment(comment).pk(pk).build();
    }
}
