package com.yss.metadata.repository;

import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.infrastructure.convertor.LineageEdgeConvertor;
import com.yss.metadata.repository.gateway.impl.LineageGraphRepositoryImpl;
import com.yss.metadata.repository.entity.LineageEdgePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 血缘图谱仓储 H2 持久化测试（WU-03-01；邻接表 CRUD/图谱查询/图版本）。
 */
class LineageGraphRepositoryImplH2Test extends H2MapperTestSupport {

    private LineageGraphRepository graphRepository;
    private LineageEdgeRepository edgeRepository;

    @BeforeEach
    void setUp() {
        edgeRepository = sqlSession.getMapper(LineageEdgeRepository.class);
        graphRepository = new LineageGraphRepositoryImpl(edgeRepository, Mappers.getMapper(LineageEdgeConvertor.class));
    }

    @Test
    @DisplayName("保存边：id 生成 + 全量图加载 + 图版本为最大 token")
    void saveAndLoadGraph() {
        LineageEdge saved = graphRepository.save(edge(null, "a-1", "a-2", "v1"));
        graphRepository.save(edge(null, "a-2", "a-3", "v2"));

        assertThat(saved.getId()).isNotBlank();
        LineageGraph graph = graphRepository.loadGraph();
        assertThat(graph.getEdges()).hasSize(2);
        assertThat(graph.getVersion()).isEqualTo("v2");
    }

    @Test
    @DisplayName("图谱查询：from/to 双向邻域 + confidence 筛选")
    void findGraphNeighborhoodAndFilter() {
        edgeRepository.insert(po("e1", "a-up", "a-center", LineageType.SQL.getValue(), LineageConfidence.AUTO_HIGH.getValue(), "v2"));
        edgeRepository.insert(po("e2", "a-center", "a-down", LineageType.SQL.getValue(), LineageConfidence.AUTO_MID.getValue(), "v2"));
        edgeRepository.insert(po("e3", "a-center", "a-down-2", LineageType.MANUAL.getValue(), LineageConfidence.MANUAL_HIGH.getValue(), "v2"));
        edgeRepository.insert(po("e4", "a-x", "a-y", LineageType.SQL.getValue(), LineageConfidence.AUTO_HIGH.getValue(), "v1"));

        LineageGraph all = graphRepository.findGraph("a-center", null);
        assertThat(all.getEdges()).extracting(LineageEdge::getId)
                .containsExactlyInAnyOrder("e1", "e2", "e3");

        LineageGraph filtered = graphRepository.findGraph("a-center", LineageConfidence.MANUAL_HIGH);
        assertThat(filtered.getEdges()).extracting(LineageEdge::getId)
                .containsExactly("e3");

        // 图版本为全局最新
        assertThat(all.getVersion()).isEqualTo("v2");
    }

    @Test
    @DisplayName("空血缘：空边结构 + 版本 null（非错误）")
    void emptyGraphReturnsEmpty() {
        LineageGraph graph = graphRepository.findGraph("a-center", null);

        assertThat(graph.getEdges()).isEmpty();
        assertThat(graph.getVersion()).isNull();
    }

    @Test
    @DisplayName("持久化 roundtrip：字段完整（类型/置信度/备注/版本）")
    void roundTripFields() {
        LineageEdge saved = graphRepository.save(edge(null, "a-1", "a-2", "v9"));
        saved.setRemark("人工确认");

        LineageGraph graph = graphRepository.loadGraph();
        LineageEdge loaded = graph.getEdges().get(0);
        assertThat(loaded.getFromAssetId()).isEqualTo("a-1");
        assertThat(loaded.getToAssetId()).isEqualTo("a-2");
        assertThat(loaded.getType()).isEqualTo(LineageType.MANUAL);
        assertThat(loaded.getConfidence()).isEqualTo(LineageConfidence.MANUAL_HIGH);
        assertThat(loaded.getGraphVersion()).isEqualTo("v9");
    }

    @Test
    @DisplayName("字段级血缘边：fromColumnId/toColumnId/transformExpr/exprType 持久化与映射")
    void testColumnLineagePersistence() {
        LineageEdge colEdge = LineageEdge.builder()
                .fromAssetId("ast-src")
                .toAssetId("ast-tgt")
                .fromColumnId("col-amt")
                .toColumnId("col-total-amt")
                .transformExpr("sum(amount)")
                .exprType("AGGREGATE")
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .remark("SQL AST 自动抽取")
                .graphVersion("v3")
                .build();

        LineageEdge saved = graphRepository.save(colEdge);
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getFromColumnId()).isEqualTo("col-amt");
        assertThat(saved.getToColumnId()).isEqualTo("col-total-amt");
        assertThat(saved.getTransformExpr()).isEqualTo("sum(amount)");
        assertThat(saved.getExprType()).isEqualTo("AGGREGATE");

        LineageGraph loaded = graphRepository.loadGraph();
        LineageEdge loadedEdge = loaded.getEdges().stream()
                .filter(e -> e.getId().equals(saved.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertThat(loadedEdge.getFromColumnId()).isEqualTo("col-amt");
        assertThat(loadedEdge.getToColumnId()).isEqualTo("col-total-amt");
        assertThat(loadedEdge.getTransformExpr()).isEqualTo("sum(amount)");
        assertThat(loadedEdge.getExprType()).isEqualTo("AGGREGATE");
    }

    private LineageEdge edge(String id, String from, String to, String version) {
        return LineageEdge.builder()
                .id(id)
                .fromAssetId(from)
                .toAssetId(to)
                .type(LineageType.MANUAL)
                .confidence(LineageConfidence.MANUAL_HIGH)
                .graphVersion(version)
                .build();
    }

    private LineageEdgePO po(String id, String from, String to, String type, String conf, String ver) {
        return LineageEdgePO.builder()
                .id(id)
                .fromAsset(from)
                .toAsset(to)
                .type(type)
                .confidence(conf)
                .graphVersion(ver)
                .build();
    }
}
