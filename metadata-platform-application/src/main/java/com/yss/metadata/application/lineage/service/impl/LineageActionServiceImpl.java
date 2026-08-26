package com.yss.metadata.application.lineage.service.impl;

import com.yss.metadata.application.lineage.service.LineageActionService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.client.dto.cmd.LineageManualCmd;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 人工补录血缘应用服务实现（WU-03-01）。
 *
 * <p>用例边界：资产存在性（404）→ 重复边幂等返回 → 图版本 token 校验
 * （CONFLICT）→ 环检测（CYCLE，定位冲突边）→ 写入新边（新图版本 token）→
 * 审计（lineage.manual）。事务边界：人工补录单聚合事务（边写入 + 版本 token 校验）。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06 替换）：operator 为 Web 层解析的
 * X-User-Id（缺省 default-user）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineageActionServiceImpl implements LineageActionService {

    /** 人工补录审计动作 */
    private static final String AUDIT_ACTION_MANUAL = "lineage.manual";

    private final LineageGraphRepository lineageGraphRepository;
    private final AssetRepository assetRepository;
    private final AuditLogGateway auditLogRepository;
    private final LineageAppConvertor lineageAppConvertor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LineageEdgeVO addManualEdge(LineageManualCmd cmd, String operator) {
        requireAsset(cmd.getFromAssetId());
        requireAsset(cmd.getToAssetId());

        LineageGraph graph = lineageGraphRepository.loadGraph();

        // 重复边幂等：返回既有边（201 语义，无需重复写入）
        Optional<LineageEdge> existing = graph.findEdge(cmd.getFromAssetId(), cmd.getToAssetId());
        if (existing.isPresent()) {
            log.info("重复补录幂等返回，from={}, to={}", cmd.getFromAssetId(), cmd.getToAssetId());
            return lineageAppConvertor.toEdgeVO(existing.get());
        }

        // 图版本 token 乐观锁（CONFLICT；恢复路径=重读图谱拿最新 token）
        graph.ensureVersion(cmd.getGraphVersionToken());

        LineageEdge proposed = LineageEdge.builder()
                .fromAssetId(cmd.getFromAssetId())
                .toAssetId(cmd.getToAssetId())
                .type(cmd.getType())
                .confidence(cmd.getConfidence())
                .remark(cmd.getRemark())
                .build();

        // 环检测（CYCLE，定位冲突边 + 闭环路径）
        graph.ensureAcyclic(proposed);

        // 写入新边并推进图版本（新 token 由实现生成）
        proposed.setGraphVersion(UUID.randomUUID().toString());
        LineageEdge saved = lineageGraphRepository.save(proposed);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .operator(operator)
                .action(AUDIT_ACTION_MANUAL)
                .object(saved.getId())
                .result("success")
                .time(LocalDateTime.now())
                .build());
        log.info("人工补录血缘成功，edgeId={}, from={}, to={}, operator={}",
                saved.getId(), saved.getFromAssetId(), saved.getToAssetId(), operator);
        return lineageAppConvertor.toEdgeVO(saved);
    }

    private void requireAsset(String assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }
}
