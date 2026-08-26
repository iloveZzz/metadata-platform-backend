package com.yss.metadata.application.lineage.service.impl;

import com.yss.metadata.application.lineage.service.ColumnLineageAppService;
import com.yss.metadata.application.lineage.service.convertor.LineageAppConvertor;
import com.yss.metadata.client.dto.cmd.ColumnLineageManualCmd;
import com.yss.metadata.client.vo.ColumnLineageGraphVO;
import com.yss.metadata.client.vo.ColumnLineageNodeVO;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.audit.gateway.AuditLogGateway;
import com.yss.metadata.domain.audit.model.AuditLogEntry;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import com.yss.metadata.domain.lineage.model.LineageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 字段级血缘应用服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnLineageAppServiceImpl implements ColumnLineageAppService {

    private static final String AUDIT_ACTION_MANUAL = "lineage.column.manual";
    private static final String AUDIT_ACTION_DELETE = "lineage.column.delete";

    public static final String DIR_BOTH = "BOTH";
    public static final String DIR_UPSTREAM = "UPSTREAM";
    public static final String DIR_DOWNSTREAM = "DOWNSTREAM";

    private final LineageGraphRepository lineageGraphRepository;
    private final AssetRepository assetRepository;
    private final AuditLogGateway auditLogRepository;
    private final LineageAppConvertor lineageAppConvertor;

    @Override
    @Transactional(readOnly = true)
    public ColumnLineageGraphVO getColumnLineageGraph(String assetId, String columnId, Integer depth, String direction) {
        Asset centerAsset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        int effectiveDepth = (depth == null || depth <= 0) ? 3 : depth;
        String dir = (direction == null || direction.trim().isEmpty()) ? DIR_BOTH : direction.trim().toUpperCase();

        LineageGraph graph = lineageGraphRepository.loadGraph();
        List<LineageEdge> allEdges = graph.getEdges();

        Set<LineageEdge> matchedEdges = new HashSet<>();
        Set<String> involvedAssetIds = new HashSet<>();
        involvedAssetIds.add(assetId);

        // 收集与当前资产/字段相关的血缘边
        Set<String> activeColumnKeys = new HashSet<>();
        if (columnId != null && !columnId.trim().isEmpty()) {
            activeColumnKeys.add(formatKey(assetId, columnId));
        }

        for (LineageEdge edge : allEdges) {
            if (edge.getFromColumnId() == null || edge.getToColumnId() == null) {
                continue;
            }

            boolean isFromCenter = assetId.equals(edge.getFromAssetId());
            boolean isToCenter = assetId.equals(edge.getToAssetId());

            if (columnId != null && !columnId.trim().isEmpty()) {
                if (isFromCenter && columnId.equalsIgnoreCase(edge.getFromColumnId())) {
                    if (DIR_BOTH.equals(dir) || DIR_DOWNSTREAM.equals(dir)) {
                        matchedEdges.add(edge);
                        involvedAssetIds.add(edge.getToAssetId());
                        activeColumnKeys.add(formatKey(edge.getToAssetId(), edge.getToColumnId()));
                    }
                }
                if (isToCenter && columnId.equalsIgnoreCase(edge.getToColumnId())) {
                    if (DIR_BOTH.equals(dir) || DIR_UPSTREAM.equals(dir)) {
                        matchedEdges.add(edge);
                        involvedAssetIds.add(edge.getFromAssetId());
                        activeColumnKeys.add(formatKey(edge.getFromAssetId(), edge.getFromColumnId()));
                    }
                }
            } else {
                if (isFromCenter && (DIR_BOTH.equals(dir) || DIR_DOWNSTREAM.equals(dir))) {
                    matchedEdges.add(edge);
                    involvedAssetIds.add(edge.getToAssetId());
                }
                if (isToCenter && (DIR_BOTH.equals(dir) || DIR_UPSTREAM.equals(dir))) {
                    matchedEdges.add(edge);
                    involvedAssetIds.add(edge.getFromAssetId());
                }
            }
        }

        // 多层级精准字段级扩散
        if (effectiveDepth > 1) {
            Set<String> currentKeys = new HashSet<>(activeColumnKeys);
            for (int d = 2; d <= effectiveDepth; d++) {
                Set<String> nextKeys = new HashSet<>();
                for (LineageEdge edge : allEdges) {
                    if (edge.getFromColumnId() == null || edge.getToColumnId() == null) {
                        continue;
                    }
                    if (columnId != null && !columnId.trim().isEmpty()) {
                        String fromKey = formatKey(edge.getFromAssetId(), edge.getFromColumnId());
                        String toKey = formatKey(edge.getToAssetId(), edge.getToColumnId());

                        if (currentKeys.contains(fromKey) && (DIR_BOTH.equals(dir) || DIR_DOWNSTREAM.equals(dir))) {
                            if (matchedEdges.add(edge)) {
                                nextKeys.add(toKey);
                                involvedAssetIds.add(edge.getToAssetId());
                            }
                        }
                        if (currentKeys.contains(toKey) && (DIR_BOTH.equals(dir) || DIR_UPSTREAM.equals(dir))) {
                            if (matchedEdges.add(edge)) {
                                nextKeys.add(fromKey);
                                involvedAssetIds.add(edge.getFromAssetId());
                            }
                        }
                    } else {
                        if (involvedAssetIds.contains(edge.getFromAssetId()) && (DIR_BOTH.equals(dir) || DIR_DOWNSTREAM.equals(dir))) {
                            if (matchedEdges.add(edge)) {
                                involvedAssetIds.add(edge.getToAssetId());
                            }
                        }
                        if (involvedAssetIds.contains(edge.getToAssetId()) && (DIR_BOTH.equals(dir) || DIR_UPSTREAM.equals(dir))) {
                            if (matchedEdges.add(edge)) {
                                involvedAssetIds.add(edge.getFromAssetId());
                            }
                        }
                    }
                }
                currentKeys = nextKeys;
            }
        }

        // 构建节点列表
        List<ColumnLineageNodeVO> nodes = new ArrayList<>();
        for (String aId : involvedAssetIds) {
            Optional<Asset> opt = assetRepository.findById(aId);
            if (opt.isPresent()) {
                Asset asset = opt.get();
                List<AssetColumn> columns = assetRepository.findColumns(aId);
                for (AssetColumn col : columns) {
                    nodes.add(ColumnLineageNodeVO.builder()
                            .assetId(asset.getId())
                            .assetName(asset.getName())
                            .tableName(asset.getName())
                            .columnId(col.getId() != null ? col.getId() : col.getName())
                            .columnName(col.getName())
                            .dataType(col.getType())
                            .classification(col.getClassification())
                            .isPrimaryKey(col.getPk() != null && col.getPk())
                            .build());
                }
            }
        }

        List<LineageEdgeVO> edgeVOs = lineageAppConvertor.toEdgeVOList(new ArrayList<>(matchedEdges));

        return ColumnLineageGraphVO.builder()
                .centerAssetId(assetId)
                .centerColumnId(columnId)
                .nodes(nodes)
                .edges(edgeVOs)
                .build();
    }

    private String formatKey(String assetId, String columnId) {
        return assetId + ":" + (columnId != null ? columnId.toLowerCase() : "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LineageEdgeVO addManualColumnEdge(ColumnLineageManualCmd cmd, String operator) {
        Asset fromAsset = assetRepository.findById(cmd.getFromAssetId())
                .orElseThrow(() -> new AssetNotFoundException(cmd.getFromAssetId()));
        Asset toAsset = assetRepository.findById(cmd.getToAssetId())
                .orElseThrow(() -> new AssetNotFoundException(cmd.getToAssetId()));

        LineageGraph graph = lineageGraphRepository.loadGraph();

        // 图版本乐观锁校验
        graph.ensureVersion(cmd.getGraphVersionToken());

        LineageEdge proposed = LineageEdge.builder()
                .fromAssetId(cmd.getFromAssetId())
                .fromColumnId(cmd.getFromColumnId())
                .toAssetId(cmd.getToAssetId())
                .toColumnId(cmd.getToColumnId())
                .transformExpr(cmd.getTransformExpr())
                .exprType(cmd.getExprType() != null ? cmd.getExprType() : "MANUAL")
                .type(LineageType.MANUAL)
                .confidence(LineageConfidence.MANUAL_HIGH)
                .remark(cmd.getRemark())
                .build();

        // 字段级防环校验
        graph.ensureColumnAcyclic(proposed);

        // 推进图版本
        proposed.setGraphVersion(UUID.randomUUID().toString());
        LineageEdge saved = lineageGraphRepository.save(proposed);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .action(AUDIT_ACTION_MANUAL)
                .object(saved.getId())
                .operator(operator != null ? operator : "default-user")
                .result("success")
                .time(LocalDateTime.now())
                .build());

        return lineageAppConvertor.toEdgeVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteColumnEdge(String edgeId, String operator) {
        lineageGraphRepository.deleteById(edgeId);

        auditLogRepository.record(AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .action(AUDIT_ACTION_DELETE)
                .object(edgeId)
                .operator(operator != null ? operator : "default-user")
                .result("success")
                .time(LocalDateTime.now())
                .build());
    }
}
