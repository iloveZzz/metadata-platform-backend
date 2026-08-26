package com.yss.metadata.application.lineage.service.impl;

import com.yss.metadata.application.lineage.service.ColumnImpactAnalysisService;
import com.yss.metadata.client.vo.AffectedColumnVO;
import com.yss.metadata.client.vo.ColumnImpactAnalysisVO;
import com.yss.metadata.client.vo.ColumnImpactLayerVO;
import com.yss.metadata.client.vo.ColumnImpactSummaryVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.gateway.AssetRepository;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 字段级爆炸半径影响分析应用服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnImpactAnalysisServiceImpl implements ColumnImpactAnalysisService {

    private static final Set<String> CRITICAL_CLASSIFICATIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("S3", "S4", "高度敏感", "核心商密", "绝密")));

    private final LineageGraphRepository lineageGraphRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public ColumnImpactAnalysisVO analyzeImpact(String assetId, String columnId, Integer maxDepth) {
        Asset srcAsset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        List<AssetColumn> srcColumns = assetRepository.findColumns(assetId);
        String srcColName = columnId;
        for (AssetColumn col : srcColumns) {
            if (columnId.equalsIgnoreCase(col.getId()) || columnId.equalsIgnoreCase(col.getName())) {
                srcColName = col.getName();
                break;
            }
        }

        LineageGraph graph = lineageGraphRepository.loadGraph();
        int searchDepth = (maxDepth == null || maxDepth <= 0) ? 5 : maxDepth;
        Map<Integer, List<LineageEdge>> depthEdges = graph.findDownstreamColumnEdges(assetId, columnId, searchDepth);

        Set<String> affectedAssetIds = new HashSet<>();
        Set<String> affectedColumnKeys = new HashSet<>();
        List<ColumnImpactLayerVO> layers = new ArrayList<>();
        boolean hasCritical = false;
        int maxReachedDepth = 0;

        for (Map.Entry<Integer, List<LineageEdge>> entry : depthEdges.entrySet()) {
            int depth = entry.getKey();
            if (depth > maxReachedDepth) {
                maxReachedDepth = depth;
            }
            List<AffectedColumnVO> affectedColsInLayer = new ArrayList<>();

            for (LineageEdge edge : entry.getValue()) {
                String toAssetId = edge.getToAssetId();
                String toColId = edge.getToColumnId();
                affectedAssetIds.add(toAssetId);
                affectedColumnKeys.add(toAssetId + ":" + toColId.toLowerCase());

                Optional<Asset> targetAssetOpt = assetRepository.findById(toAssetId);
                String targetAssetName = targetAssetOpt.map(Asset::getName).orElse(toAssetId);

                List<AssetColumn> targetCols = assetRepository.findColumns(toAssetId);
                String colDataType = null;
                String colClassification = null;
                String actualColName = toColId;

                for (AssetColumn targetCol : targetCols) {
                    if (toColId.equalsIgnoreCase(targetCol.getId()) || toColId.equalsIgnoreCase(targetCol.getName())) {
                        actualColName = targetCol.getName();
                        colDataType = targetCol.getType();
                        colClassification = targetCol.getClassification();
                        break;
                    }
                }

                if (colClassification != null && CRITICAL_CLASSIFICATIONS.contains(colClassification)) {
                    hasCritical = true;
                }

                affectedColsInLayer.add(AffectedColumnVO.builder()
                        .assetId(toAssetId)
                        .assetName(targetAssetName)
                        .columnId(toColId)
                        .columnName(actualColName)
                        .dataType(colDataType)
                        .transformExpr(edge.getTransformExpr())
                        .exprType(edge.getExprType())
                        .classification(colClassification)
                        .build());
            }

            layers.add(ColumnImpactLayerVO.builder()
                    .depth(depth)
                    .affectedColumns(affectedColsInLayer)
                    .build());
        }

        ColumnImpactSummaryVO summary = ColumnImpactSummaryVO.builder()
                .totalAffectedAssets(affectedAssetIds.size())
                .totalAffectedColumns(affectedColumnKeys.size())
                .maxDepth(maxReachedDepth)
                .hasCriticalDownstream(hasCritical)
                .build();

        return ColumnImpactAnalysisVO.builder()
                .sourceAssetId(srcAsset.getId())
                .sourceAssetName(srcAsset.getName())
                .sourceColumnId(columnId)
                .sourceColumnName(srcColName)
                .impactSummary(summary)
                .impactLayers(layers)
                .build();
    }
}
