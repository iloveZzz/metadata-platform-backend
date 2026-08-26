package com.yss.metadata.infrastructure.dq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yss.metadata.domain.dq.gateway.BlastRadiusGateway;
import com.yss.metadata.domain.dq.model.BlastRadiusAsset;
import com.yss.metadata.domain.dq.model.BlastRadiusReport;
import com.yss.metadata.repository.AssetRepository;
import com.yss.metadata.repository.LineageEdgeRepository;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.LineageEdgePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 下游爆炸半径递归计算网关实现
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlastRadiusGatewayImpl implements BlastRadiusGateway {

    private final LineageEdgeRepository lineageEdgeRepository;
    private final AssetRepository assetRepository;

    @Override
    public BlastRadiusReport calculateBlastRadius(String originAssetId, int maxDepth) {
        int depthLimit = maxDepth <= 0 ? 5 : maxDepth;
        AssetPO originAsset = assetRepository.selectById(originAssetId);
        String originName = originAsset != null ? originAsset.getName() : originAssetId;

        // BFS 向下递归遍历（防循环引用，使用 visited 集合）
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthMap = new HashMap<>();

        queue.add(originAssetId);
        visited.add(originAssetId);
        depthMap.put(originAssetId, 0);

        List<BlastRadiusAsset> impactedList = new ArrayList<>();
        int currentMaxDepth = 0;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depthMap.get(current);

            if (currentDepth >= depthLimit) {
                continue;
            }

            // 查询以 current 为上游的所有下游边 (fromAsset = current)
            LambdaQueryWrapper<LineageEdgePO> wrapper = new LambdaQueryWrapper<LineageEdgePO>()
                    .eq(LineageEdgePO::getFromAsset, current);
            List<LineageEdgePO> downstreamEdges = lineageEdgeRepository.selectList(wrapper);

            if (downstreamEdges != null && !downstreamEdges.isEmpty()) {
                for (LineageEdgePO edge : downstreamEdges) {
                    String downstreamId = edge.getToAsset();
                    if (!visited.contains(downstreamId)) {
                        visited.add(downstreamId);
                        int nextDepth = currentDepth + 1;
                        depthMap.put(downstreamId, nextDepth);
                        queue.add(downstreamId);

                        if (nextDepth > currentMaxDepth) {
                            currentMaxDepth = nextDepth;
                        }

                        AssetPO po = assetRepository.selectById(downstreamId);
                        String name = po != null ? po.getName() : downstreamId;
                        String domain = po != null ? po.getDomain() : "default";
                        String owner = po != null ? po.getOwner() : "未认领";
                        String taintStatus = po != null && po.getTaintStatus() != null ? po.getTaintStatus() : "NORMAL";

                        // 质量分模拟递减扩散
                        int healthScore = Math.max(40, 85 - nextDepth * 10);
                        String qualityBand = healthScore >= 75 ? "good" : (healthScore >= 60 ? "fair" : "poor");

                        impactedList.add(BlastRadiusAsset.builder()
                                .assetId(downstreamId)
                                .assetName(name)
                                .title(name)
                                .domain(domain)
                                .depth(nextDepth)
                                .owner(owner)
                                .healthScore(healthScore)
                                .qualityBand(qualityBand)
                                .taintStatus(taintStatus)
                                .build());
                    }
                }
            }
        }

        List<String> domains = impactedList.stream()
                .map(BlastRadiusAsset::getDomain)
                .filter(d -> d != null && !d.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        return BlastRadiusReport.builder()
                .originAssetId(originAssetId)
                .originAssetName(originName)
                .impactedAssets(impactedList)
                .totalImpactedCount(impactedList.size())
                .maxDepth(currentMaxDepth)
                .impactedDomains(domains)
                .build();
    }
}
