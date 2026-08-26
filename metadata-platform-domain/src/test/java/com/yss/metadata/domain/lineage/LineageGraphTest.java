package com.yss.metadata.domain.lineage;

import com.yss.metadata.domain.lineage.exception.LineageConflictException;
import com.yss.metadata.domain.lineage.exception.LineageCycleException;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 血缘图聚合行为测试（WU-03-01，TDD 红→绿）。
 *
 * <p>覆盖：环检测 CYCLE（从 to→from 追踪并定位冲突边）、自环、无环放行、
 * 图版本 token 乐观锁 CONFLICT（缺省跳过/匹配放行）、重复边幂等语义。</p>
 */
class LineageGraphTest {

    // ---------- 环检测（CYCLE） ----------

    @Test
    @DisplayName("成环阻断：补录 C→A（存在 A→B→C）抛 CYCLE 并定位冲突边与闭环路径")
    void cycleDetectedAndLocateConflictEdge() {
        LineageGraph graph = LineageGraph.of(Arrays.asList(
                edge("e1", "A", "B", "v0"),
                edge("e2", "B", "C", "v0")), "v0");
        LineageEdge proposed = manualEdge("C", "A", "v0");

        assertThatThrownBy(() -> graph.ensureAcyclic(proposed))
                .isInstanceOf(LineageCycleException.class)
                .satisfies(ex -> {
                    LineageCycleException cycle = (LineageCycleException) ex;
                    assertThat(cycle.getErrCode()).isEqualTo("lineage.cycle");
                    assertThat(cycle.getConflictEdge().getFromAssetId()).isEqualTo("C");
                    assertThat(cycle.getConflictEdge().getToAssetId()).isEqualTo("A");
                    // 闭环路径：既有边 A→B、B→C（从 to=A 出发追踪到 from=C）
                    assertThat(cycle.getCyclePath()).extracting(LineageEdge::getId)
                            .containsExactly("e1", "e2");
                    assertThat(cycle.getMessage()).contains("C→A")
                            .contains("e1").contains("e2");
                });
    }

    @Test
    @DisplayName("反向边构成二元环：补录 A→B（已有 B→A）抛 CYCLE")
    void reverseEdgeFormsTwoCycle() {
        LineageGraph graph = LineageGraph.of(Collections.singletonList(
                edge("e1", "B", "A", "v0")), "v0");

        assertThatThrownBy(() -> graph.ensureAcyclic(manualEdge("A", "B", "v0")))
                .isInstanceOf(LineageCycleException.class)
                .satisfies(ex -> {
                    LineageCycleException cycle = (LineageCycleException) ex;
                    assertThat(cycle.getCyclePath()).extracting(LineageEdge::getId)
                            .containsExactly("e1");
                });
    }

    @Test
    @DisplayName("自环阻断：补录 A→A 抛 CYCLE（冲突边即自身，无既有路径）")
    void selfLoopDetected() {
        LineageGraph graph = LineageGraph.of(Collections.emptyList(), "v0");

        assertThatThrownBy(() -> graph.ensureAcyclic(manualEdge("A", "A", "v0")))
                .isInstanceOf(LineageCycleException.class)
                .satisfies(ex -> {
                    LineageCycleException cycle = (LineageCycleException) ex;
                    assertThat(cycle.getConflictEdge().getFromAssetId()).isEqualTo("A");
                    assertThat(cycle.getCyclePath()).isEmpty();
                });
    }

    @Test
    @DisplayName("无环补录放行：新增边不形成环时不抛异常")
    void acyclicEdgeAllowed() {
        LineageGraph graph = LineageGraph.of(Arrays.asList(
                edge("e1", "A", "B", "v0"),
                edge("e2", "B", "C", "v0")), "v0");

        graph.ensureAcyclic(manualEdge("A", "D", "v0"));
        graph.ensureAcyclic(manualEdge("D", "E", "v0"));
    }

    @Test
    @DisplayName("findCyclePath 只沿下游追踪：无关孤岛边不影响判定")
    void cyclePathOnlyFollowsDownstream() {
        // 孤岛边 X→Y 与候选边无关；无 A/B 关联边时补录 B→A、A→B 均不成环
        LineageGraph graph = LineageGraph.of(Collections.singletonList(
                edge("e1", "X", "Y", "v0")), "v0");

        assertThat(graph.findCyclePath("B", "A")).isEmpty();
        assertThat(graph.findCyclePath("A", "B")).isEmpty();
    }

    // ---------- 图版本 token 乐观锁（CONFLICT） ----------

    @Test
    @DisplayName("token 匹配当前版本放行；不匹配抛 CONFLICT")
    void versionTokenCheck() {
        LineageGraph graph = LineageGraph.of(Collections.emptyList(), "v1");

        graph.ensureVersion("v1");

        assertThatThrownBy(() -> graph.ensureVersion("stale-token"))
                .isInstanceOf(LineageConflictException.class)
                .hasMessageContaining("CONFLICT");
    }

    @Test
    @DisplayName("token 缺省（null/空白）跳过版本校验（OpenAPI 字段可选）")
    void missingTokenSkipped() {
        LineageGraph graph = LineageGraph.of(Collections.emptyList(), "v1");

        graph.ensureVersion(null);
        graph.ensureVersion("");
        graph.ensureVersion("   ");
    }

    @Test
    @DisplayName("空图（version=null）时非空 token 视为过期返回 CONFLICT")
    void staleTokenOnEmptyGraphRejected() {
        LineageGraph graph = LineageGraph.of(Collections.emptyList(), null);

        assertThatThrownBy(() -> graph.ensureVersion("stale"))
                .isInstanceOf(LineageConflictException.class);
    }

    // ---------- 重复边 / 图快照 ----------

    @Test
    @DisplayName("重复边可被识别（幂等语义由应用层处理）")
    void duplicateEdgeDetected() {
        LineageGraph graph = LineageGraph.of(Arrays.asList(
                edge("e1", "A", "B", "v0")), "v0");

        assertThat(graph.contains("A", "B")).isTrue();
        assertThat(graph.findEdge("A", "B")).isPresent();
        assertThat(graph.findEdge("B", "A")).isEmpty();
    }

    @Test
    @DisplayName("图快照携带版本与边集合（图谱查询 VO 组装依据）")
    void graphSnapshotCarriesVersionAndEdges() {
        LineageGraph graph = LineageGraph.of(Collections.singletonList(
                edge("e1", "A", "B", "v1")), "v1");

        // 缺省/空白 token 放行
        graph.ensureVersion(null);
        graph.ensureVersion("");
        graph.ensureVersion("   ");

        // 匹配放行
        graph.ensureVersion("v1");

        // 不匹配抛 CONFLICT
        assertThatThrownBy(() -> graph.ensureVersion("v0"))
                .isInstanceOf(LineageConflictException.class)
                .hasMessageContaining("图版本冲突");
    }

    // ---------- 字段级血缘与爆炸半径 ----------

    @Test
    @DisplayName("字段级成环阻断：colA -> colB -> colC，补录 colC -> colA 阻断")
    void testColumnCycleDetection() {
        LineageEdge e1 = LineageEdge.builder()
                .id("e1").fromAssetId("ast1").fromColumnId("colA").toAssetId("ast2").toColumnId("colB")
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build();
        LineageEdge e2 = LineageEdge.builder()
                .id("e2").fromAssetId("ast2").fromColumnId("colB").toAssetId("ast3").toColumnId("colC")
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build();

        LineageGraph graph = LineageGraph.of(Arrays.asList(e1, e2), "v1");

        LineageEdge cycleEdge = LineageEdge.builder()
                .id("e-new").fromAssetId("ast3").fromColumnId("colC").toAssetId("ast1").toColumnId("colA")
                .type(LineageType.MANUAL).confidence(LineageConfidence.MANUAL_HIGH).build();

        assertThatThrownBy(() -> graph.ensureColumnAcyclic(cycleEdge))
                .isInstanceOf(LineageCycleException.class);
    }

    @Test
    @DisplayName("字段级下游爆炸半径 BFS 遍历：多层级扩散与深度分组")
    void testFindDownstreamColumnEdges() {
        // ast1.c1 -> ast2.c2 (depth 1)
        // ast2.c2 -> ast3.c3 (depth 2)
        // ast3.c3 -> ast4.c4 (depth 3)
        LineageEdge e1 = LineageEdge.builder()
                .id("e1").fromAssetId("ast1").fromColumnId("c1").toAssetId("ast2").toColumnId("c2")
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build();
        LineageEdge e2 = LineageEdge.builder()
                .id("e2").fromAssetId("ast2").fromColumnId("c2").toAssetId("ast3").toColumnId("c3")
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build();
        LineageEdge e3 = LineageEdge.builder()
                .id("e3").fromAssetId("ast3").fromColumnId("c3").toAssetId("ast4").toColumnId("c4")
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build();

        LineageGraph graph = LineageGraph.of(Arrays.asList(e1, e2, e3), "v1");

        Map<Integer, List<LineageEdge>> depthMap = graph.findDownstreamColumnEdges("ast1", "c1", 5);
        assertThat(depthMap).hasSize(3);
        assertThat(depthMap.get(1)).extracting(e -> e.getId()).containsExactly("e1");
        assertThat(depthMap.get(2)).extracting(e -> e.getId()).containsExactly("e2");
        assertThat(depthMap.get(3)).extracting(e -> e.getId()).containsExactly("e3");
    }

    // ---------- Helper ----------

    private static LineageEdge edge(String id, String from, String to, String version) {
        return LineageEdge.builder()
                .id(id)
                .fromAssetId(from)
                .toAssetId(to)
                .type(LineageType.SQL)
                .confidence(LineageConfidence.AUTO_HIGH)
                .graphVersion(version)
                .build();
    }

    private static LineageEdge manualEdge(String from, String to, String version) {
        return LineageEdge.builder()
                .fromAssetId(from)
                .toAssetId(to)
                .type(LineageType.MANUAL)
                .confidence(LineageConfidence.MANUAL_HIGH)
                .graphVersion(version)
                .build();
    }
}
