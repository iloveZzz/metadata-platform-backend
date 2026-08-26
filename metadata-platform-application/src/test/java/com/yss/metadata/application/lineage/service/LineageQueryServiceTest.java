package com.yss.metadata.application.lineage.service;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.lineage.service.impl.LineageQueryServiceImpl;
import com.yss.metadata.application.lineage.support.InMemoryLineageGraphRepository;
import com.yss.metadata.client.vo.LineageGraphVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 血缘图谱查询应用服务测试（WU-03-01 图谱查询）。
 *
 * <p>覆盖：邻域查询（from/to 双向）、confidence 筛选（all 不过滤）、
 * 空血缘空结构、404、非法 confidence 422。</p>
 */
class LineageQueryServiceTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryLineageGraphRepository graphRepository;
    private LineageQueryService queryService;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        graphRepository = new InMemoryLineageGraphRepository();
        queryService = new LineageQueryServiceImpl(assetRepository, graphRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
    }

    @Test
    @DisplayName("图谱查询返回中心资产邻域边（from/to 双向）与图版本 token")
    void graphReturnsNeighborhoodAndVersion() {
        seedAsset("a-center", "dwd_order_di");
        graphRepository.seed(edge("e1", "a-up", "a-center", "v3"));
        graphRepository.seed(edge("e2", "a-center", "a-down", "v2"));
        graphRepository.seed(edge("e3", "a-x", "a-y", "v1"));

        LineageGraphVO vo = queryService.getGraph("a-center", "all");

        assertThat(vo.getEdges()).hasSize(2);
        assertThat(vo.getEdges()).extracting(e -> e.getId()).containsExactlyInAnyOrder("e1", "e2");
        // 图版本为全局最新（跨资产邻域查询也返回全局 token，供补录乐观锁）
        assertThat(vo.getGraphVersionToken()).isEqualTo("v3");
    }

    @Test
    @DisplayName("confidence 筛选：仅返回匹配置信度边")
    void graphConfidenceFilter() {
        seedAsset("a-center", "dwd_order_di");
        graphRepository.seed(edge("e1", "a-center", "a-down-1", "v2", LineageConfidence.AUTO_HIGH));
        graphRepository.seed(edge("e2", "a-center", "a-down-2", "v2", LineageConfidence.MANUAL_HIGH));

        LineageGraphVO vo = queryService.getGraph("a-center", "manual-high");

        assertThat(vo.getEdges()).extracting(e -> e.getId()).containsExactly("e2");
        assertThat(vo.getEdges().get(0).getConfidence()).isEqualTo("manual-high");
    }

    @Test
    @DisplayName("空血缘返回空结构（200 语义，非错误）")
    void emptyGraphReturnsEmptyStructure() {
        seedAsset("a-center", "dwd_order_di");

        LineageGraphVO vo = queryService.getGraph("a-center", "all");

        assertThat(vo.getEdges()).isEmpty();
        assertThat(vo.getGraphVersionToken()).isNull();
    }

    @Test
    @DisplayName("资产不存在抛未找到（404 语义）")
    void assetNotFoundThrows() {
        assertThatThrownBy(() -> queryService.getGraph("not-exist", "all"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    @DisplayName("非法 confidence 抛非法参数（422 语义）")
    void invalidConfidenceThrows() {
        seedAsset("a-center", "dwd_order_di");

        assertThatThrownBy(() -> queryService.getGraph("a-center", "unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void seedAsset(String id, String name) {
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification("内部").status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private LineageEdge edge(String id, String from, String to, String version) {
        return edge(id, from, to, version, LineageConfidence.AUTO_HIGH);
    }

    private LineageEdge edge(String id, String from, String to, String version,
                             LineageConfidence confidence) {
        return LineageEdge.builder().id(id).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(confidence).graphVersion(version).build();
    }
}
