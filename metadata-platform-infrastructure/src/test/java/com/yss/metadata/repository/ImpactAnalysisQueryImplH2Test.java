package com.yss.metadata.repository;

import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.infrastructure.convertor.ImpactHitConvertor;
import com.yss.metadata.repository.gateway.impl.ImpactAnalysisQueryImpl;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.LineageEdgePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 影响分析递归 CTE H2 测试（WU-03-03；原生 SQL 人工确认项）。
 *
 * <p>覆盖：下游全量召回 + 深度 + 环保护（成环图终止）+ 深度上限 + 0 影响空列表；
 * H2 2.1.214 兼容（WITH RECURSIVE / POSITION / CONCAT）。</p>
 */
class ImpactAnalysisQueryImplH2Test extends H2MapperTestSupport {

    private ImpactAnalysisRepository impactRepository;
    private LineageEdgeRepository edgeRepository;
    private AssetRepository assetRepository;

    @BeforeEach
    void setUp() {
        edgeRepository = sqlSession.getMapper(LineageEdgeRepository.class);
        assetRepository = sqlSession.getMapper(AssetRepository.class);
        impactRepository = new ImpactAnalysisQueryImpl(
                sqlSession.getMapper(LineageImpactMapper.class), Mappers.getMapper(ImpactHitConvertor.class));
    }

    @Test
    @DisplayName("下游全量召回：链式下游带深度")
    void downstreamFullRecallWithDepth() {
        seedAsset("a-root", "dwd_order_di");
        seedAsset("a-1", "ads_order_1d");
        seedAsset("a-2", "bi_report_2d");
        seedEdge("e1", "a-root", "a-1");
        seedEdge("e2", "a-1", "a-2");

        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 10);

        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(ImpactNode::getAssetId)
                .containsExactlyInAnyOrder("a-1", "a-2");
        assertThat(nodes).anySatisfy(node -> {
            assertThat(node.getAssetId()).isEqualTo("a-1");
            assertThat(node.getDepth()).isEqualTo(1);
            assertThat(node.getName()).isEqualTo("ads_order_1d");
        });
        assertThat(nodes).anySatisfy(node -> {
            assertThat(node.getAssetId()).isEqualTo("a-2");
            assertThat(node.getDepth()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("扇出召回：多分支下游全部命中")
    void downstreamFanOut() {
        seedAsset("a-root", "dwd_order_di");
        seedAsset("a-1", "ads_1");
        seedAsset("a-2", "ads_2");
        seedAsset("a-3", "ads_3");
        seedEdge("e1", "a-root", "a-1");
        seedEdge("e2", "a-root", "a-2");
        seedEdge("e3", "a-1", "a-3");

        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 10);

        assertThat(nodes).extracting(ImpactNode::getAssetId)
                .containsExactlyInAnyOrder("a-1", "a-2", "a-3");
    }

    @Test
    @DisplayName("环保护：成环图递归终止，同路径不重复经过同一边")
    void cycleProtectionTerminates() {
        seedAsset("a-root", "dwd_order_di");
        seedAsset("a-1", "ads_1");
        seedAsset("a-2", "ads_2");
        seedEdge("e1", "a-root", "a-1");
        seedEdge("e2", "a-1", "a-2");
        seedEdge("e3", "a-2", "a-1"); // 环

        // maxDepth=20 > 边数，若无限递归将远超预期行数；路径级环保护阻断第二轮
        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 20);

        // 环仅遍历一轮：a-1 在深度 1 与 3，a-2 深度 2；a-1 不被重复召回两次以上
        assertThat(nodes).extracting(ImpactNode::getAssetId)
                .containsOnly("a-1", "a-2");
        assertThat(nodes).filteredOn(node -> node.getAssetId().equals("a-1"))
                .extracting(ImpactNode::getDepth)
                .containsExactlyInAnyOrder(1, 3);
        assertThat(nodes).filteredOn(node -> node.getAssetId().equals("a-2"))
                .extracting(ImpactNode::getDepth)
                .containsExactly(2);
    }

    @Test
    @DisplayName("深度上限：超过上限的节点不被召回")
    void depthCapEnforced() {
        seedAsset("a-root", "dwd_order_di");
        seedAsset("a-1", "ads_1");
        seedAsset("a-2", "ads_2");
        seedAsset("a-3", "ads_3");
        seedEdge("e1", "a-root", "a-1");
        seedEdge("e2", "a-1", "a-2");
        seedEdge("e3", "a-2", "a-3");

        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 2);

        assertThat(nodes).extracting(ImpactNode::getAssetId)
                .containsExactlyInAnyOrder("a-1", "a-2");
        assertThat(nodes).extracting(ImpactNode::getDepth)
                .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("0 下游：空列表（非错误）")
    void emptyDownstreamReturnsEmpty() {
        seedAsset("a-root", "dwd_order_di");

        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 10);

        assertThat(nodes).isEmpty();
    }

    @Test
    @DisplayName("资产组合字段经 LEFT JOIN 填充（缺失资产为 null）")
    void joinFillsAssetFields() {
        seedAsset("a-root", "dwd_order_di");
        seedAsset("a-1", "ads_order_1d");
        seedEdge("e1", "a-root", "a-1");
        seedEdge("e2", "a-root", "a-missing");

        List<ImpactNode> nodes = impactRepository.findDownstream("a-root", 10);

        assertThat(nodes).filteredOn(node -> node.getAssetId().equals("a-1"))
                .allSatisfy(node -> assertThat(node.getName()).isEqualTo("ads_order_1d"));
        assertThat(nodes).filteredOn(node -> node.getAssetId().equals("a-missing"))
                .allSatisfy(node -> assertThat(node.getName()).isNull());
    }

    private void seedAsset(String id, String name) {
        assetRepository.insert(buildAssetPo(id, "s-1", name, "table", "交易域", null,
                "内部", "claimed", java.time.LocalDateTime.of(2026, 8, 10, 9, 0)));
    }

    private void seedEdge(String id, String from, String to) {
        edgeRepository.insert(LineageEdgePO.builder().id(id).fromAsset(from).toAsset(to)
                .type("sql").confidence("auto-high").build());
    }
}
