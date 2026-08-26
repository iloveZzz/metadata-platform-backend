package com.yss.metadata.rest.support;

import com.yss.metadata.domain.lineage.gateway.ImpactAnalysisRepository;
import com.yss.metadata.domain.lineage.model.ImpactNode;
import com.yss.metadata.domain.lineage.model.LineageEdge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 影响分析仓储内存实现（Web 契约测试 seam；BFS 镜像递归 CTE 语义）。
 */
public class InMemoryImpactAnalysisRepository implements ImpactAnalysisRepository {

    private final List<LineageEdge> edges = new ArrayList<>();

    private final Map<String, ImpactNode> nodeMeta = new LinkedHashMap<>();

    public void seedEdge(LineageEdge edge) {
        edges.add(edge);
    }

    public void seedNode(String assetId, String name, String type, String domain, String classification) {
        nodeMeta.put(assetId, ImpactNode.builder().assetId(assetId).name(name).type(type)
                .domain(domain).classification(classification).build());
    }

    @Override
    public List<ImpactNode> findDownstream(String assetId, int maxDepth) {
        List<ImpactNode> result = new ArrayList<>();
        Deque<Cursor> queue = new ArrayDeque<>();
        queue.add(new Cursor(assetId, 0, new LinkedHashSet<>()));
        while (!queue.isEmpty()) {
            Cursor cursor = queue.poll();
            for (LineageEdge edge : edges) {
                if (!cursor.current.equals(edge.getFromAssetId())) {
                    continue;
                }
                if (cursor.visited.contains(edge.getId())) {
                    continue;
                }
                int nextDepth = cursor.depth + 1;
                if (nextDepth > maxDepth) {
                    continue;
                }
                ImpactNode node = buildNode(edge.getToAssetId(), nextDepth);
                if (node != null) {
                    result.add(node);
                }
                Set<String> nextVisited = new LinkedHashSet<>(cursor.visited);
                nextVisited.add(edge.getId());
                queue.add(new Cursor(edge.getToAssetId(), nextDepth, nextVisited));
            }
        }
        return result;
    }

    private ImpactNode buildNode(String assetId, int depth) {
        ImpactNode meta = nodeMeta.get(assetId);
        if (meta == null) {
            return ImpactNode.builder().assetId(assetId).depth(depth).build();
        }
        return ImpactNode.builder().assetId(assetId).name(meta.getName()).type(meta.getType())
                .domain(meta.getDomain()).classification(meta.getClassification()).depth(depth).build();
    }

    private static final class Cursor {
        final String current;
        final int depth;
        final Set<String> visited;

        Cursor(String current, int depth, Set<String> visited) {
            this.current = current;
            this.depth = depth;
            this.visited = visited;
        }
    }
}
