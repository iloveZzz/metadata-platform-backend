package com.yss.metadata.rest.support;

import com.yss.metadata.domain.lineage.gateway.LineageGraphRepository;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 血缘图仓储内存实现（Web 契约测试 seam；镜像 LineageGraphRepositoryImpl 语义）。
 */
public class InMemoryLineageGraphRepository implements LineageGraphRepository {

    private final List<LineageEdge> edges = new ArrayList<>();

    public void seed(LineageEdge edge) {
        edges.add(edge);
    }

    public List<LineageEdge> allEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public LineageGraph loadGraph() {
        return LineageGraph.of(new ArrayList<>(edges), latestVersion());
    }

    @Override
    public LineageGraph findGraph(String assetId, LineageConfidence confidence) {
        List<LineageEdge> neighborhood = edges.stream()
                .filter(e -> assetId.equals(e.getFromAssetId()) || assetId.equals(e.getToAssetId()))
                .filter(e -> confidence == null || confidence == e.getConfidence())
                .collect(Collectors.toList());
        return LineageGraph.of(neighborhood, latestVersion());
    }

    @Override
    public LineageEdge save(LineageEdge edge) {
        if (edge.getId() == null || edge.getId().trim().isEmpty()) {
            edge.setId(UUID.randomUUID().toString());
        }
        edges.add(edge);
        return edge;
    }

    @Override
    public void deleteById(String edgeId) {
        if (edgeId != null) {
            edges.removeIf(e -> edgeId.equals(e.getId()));
        }
    }

    private String latestVersion() {
        return edges.stream().map(LineageEdge::getGraphVersion)
                .filter(Objects::nonNull)
                .max(String::compareTo)
                .orElse(null);
    }
}
