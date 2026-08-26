package com.yss.metadata.application.lineage.service;

import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.lineage.service.impl.LineageActionServiceImpl;
import com.yss.metadata.application.lineage.support.InMemoryAuditLogRepository;
import com.yss.metadata.application.lineage.support.InMemoryLineageGraphRepository;
import com.yss.metadata.client.dto.cmd.LineageManualCmd;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.lineage.exception.LineageConflictException;
import com.yss.metadata.domain.lineage.exception.LineageCycleException;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 人工补录血缘应用服务测试（WU-03-01）。
 *
 * <p>覆盖：成功 201 语义（边持久化 + 审计）、成环 CYCLE（定位冲突边）、
 * 版本 token CONFLICT、重复边幂等、资产不存在 404。</p>
 */
class LineageActionServiceTest {

    private InMemoryAssetRepository assetRepository;
    private InMemoryLineageGraphRepository graphRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private LineageActionService actionService;

    @BeforeEach
    void setUp() {
        assetRepository = new InMemoryAssetRepository();
        graphRepository = new InMemoryLineageGraphRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        actionService = new LineageActionServiceImpl(
                graphRepository, assetRepository, auditLogRepository, org.mapstruct.factory.Mappers.getMapper(LineageAppConvertor.class));
    }

    @Test
    @DisplayName("补录成功：边持久化 + 返回 VO + 审计 lineage.manual")
    void addManualEdgeSuccess() {
        seedAsset("a-from", "ods_order");
        seedAsset("a-to", "dwd_order_di");

        LineageEdgeVO vo = actionService.addManualEdge(
                cmd("a-from", "a-to", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, "人工确认"), "u-me");

        assertThat(vo.getId()).isNotBlank();
        assertThat(vo.getFromAssetId()).isEqualTo("a-from");
        assertThat(vo.getToAssetId()).isEqualTo("a-to");
        assertThat(vo.getType()).isEqualTo("manual");
        assertThat(vo.getConfidence()).isEqualTo("manual-high");
        assertThat(vo.getRemark()).isEqualTo("人工确认");
        // 图版本 token 已推进（新边携带新 token）
        LineageGraph graph = graphRepository.loadGraph();
        assertThat(graph.getVersion()).isNotBlank();
        assertThat(graph.getEdges()).hasSize(1);
        // 审计记录
        assertThat(auditLogRepository.entries()).hasSize(1);
        AuditLogEntry entry = auditLogRepository.entries().get(0);
        assertThat(entry.getAction()).isEqualTo("lineage.manual");
        assertThat(entry.getOperator()).isEqualTo("u-me");
        assertThat(entry.getObject()).isEqualTo(vo.getId());
    }

    @Test
    @DisplayName("成环补录抛 CYCLE：定位冲突边与闭环路径，不落库不审计")
    void cycleThrowsAndLocatesConflictEdge() {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        seedAsset("a-3", "t3");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v0"));
        graphRepository.seed(edge("e2", "a-2", "a-3", "v0"));

        assertThatThrownBy(() -> actionService.addManualEdge(
                cmd("a-3", "a-1", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, null), "u-me"))
                .isInstanceOf(LineageCycleException.class)
                .satisfies(ex -> {
                    LineageCycleException cycle = (LineageCycleException) ex;
                    assertThat(cycle.getConflictEdge().getFromAssetId()).isEqualTo("a-3");
                    assertThat(cycle.getCyclePath()).extracting(LineageEdge::getId)
                            .containsExactly("e1", "e2");
                });

        assertThat(graphRepository.allEdges()).hasSize(2);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("版本 token 不匹配抛 CONFLICT；匹配放行")
    void versionConflictThrows() {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        seedAsset("a-3", "t3");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v1"));

        assertThatThrownBy(() -> actionService.addManualEdge(
                cmdWithToken("a-2", "a-3", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, null, "stale"), "u-me"))
                .isInstanceOf(LineageConflictException.class)
                .hasMessageContaining("CONFLICT");

        // 带最新 token 补录成功
        LineageEdgeVO vo = actionService.addManualEdge(
                cmdWithToken("a-2", "a-3", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, null, "v1"), "u-me");
        assertThat(vo.getToAssetId()).isEqualTo("a-3");
    }

    @Test
    @DisplayName("重复边幂等：返回既有边，不重复写入不重复审计")
    void duplicateEdgeIdempotent() {
        seedAsset("a-1", "t1");
        seedAsset("a-2", "t2");
        graphRepository.seed(edge("e1", "a-1", "a-2", "v1"));

        LineageEdgeVO vo = actionService.addManualEdge(
                cmd("a-1", "a-2", LineageType.SQL, LineageConfidence.AUTO_HIGH, null), "u-me");

        assertThat(vo.getId()).isEqualTo("e1");
        assertThat(graphRepository.allEdges()).hasSize(1);
        assertThat(auditLogRepository.entries()).isEmpty();
    }

    @Test
    @DisplayName("资产不存在（from/to）抛未找到（404 语义）")
    void assetNotFoundThrows() {
        seedAsset("a-1", "t1");

        assertThatThrownBy(() -> actionService.addManualEdge(
                cmd("not-exist", "a-1", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, null), "u-me"))
                .isInstanceOf(AssetNotFoundException.class);
        assertThatThrownBy(() -> actionService.addManualEdge(
                cmd("a-1", "not-exist", LineageType.MANUAL, LineageConfidence.MANUAL_HIGH, null), "u-me"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    private void seedAsset(String id, String name) {
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification("内部").status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private LineageManualCmd cmd(String from, String to, LineageType type,
                                 LineageConfidence confidence, String remark) {
        return cmdWithToken(from, to, type, confidence, remark, null);
    }

    private LineageManualCmd cmdWithToken(String from, String to, LineageType type,
                                          LineageConfidence confidence, String remark, String token) {
        LineageManualCmd cmd = new LineageManualCmd();
        cmd.setFromAssetId(from);
        cmd.setToAssetId(to);
        cmd.setType(type);
        cmd.setConfidence(confidence);
        cmd.setRemark(remark);
        cmd.setGraphVersionToken(token);
        return cmd;
    }

    private LineageEdge edge(String id, String from, String to, String version) {
        return LineageEdge.builder().id(id).fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH)
                .graphVersion(version).build();
    }
}
