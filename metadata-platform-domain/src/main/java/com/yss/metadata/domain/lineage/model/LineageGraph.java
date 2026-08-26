package com.yss.metadata.domain.lineage.model;

import com.yss.metadata.domain.lineage.exception.LineageConflictException;
import com.yss.metadata.domain.lineage.exception.LineageCycleException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 血缘图聚合（WU-03-01；数据架构：lineage_edge 邻接表 + 有向无环图）。
 *
 * <p>核心规则：
 * <ul>
 *   <li>环检测：补录 from→to 时从 to 出发沿下游（from→to 方向）追踪，
 *       可达 from 即成环，抛 {@link LineageCycleException}（CYCLE，定位冲突边 + 闭环路径）；</li>
 *   <li>图版本乐观锁：graphVersionToken 与当前图版本不匹配抛
 *       {@link LineageConflictException}（CONFLICT；恢复路径=客户端重读图谱拿最新 token）；</li>
 *   <li>重复边可识别（幂等语义由应用层处理）。</li>
 * </ul></p>
 */
public class LineageGraph {

    /** 邻接表：from → 出边（不可变） */
    private final Map<String, List<LineageEdge>> downstream;

    /** 全量边集合（不可变） */
    private final List<LineageEdge> edges;

    /** 当前图版本 token（全部边 graph_version 的最大值；空图为 null） */
    private final String version;

    private LineageGraph(List<LineageEdge> edges, String version) {
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.version = version;
        Map<String, List<LineageEdge>> map = new LinkedHashMap<>();
        for (LineageEdge edge : edges) {
            map.computeIfAbsent(edge.getFromAssetId(), k -> new ArrayList<>()).add(edge);
        }
        Map<String, List<LineageEdge>> immutable = new LinkedHashMap<>();
        map.forEach((k, v) -> immutable.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        this.downstream = Collections.unmodifiableMap(immutable);
    }

    public static LineageGraph of(List<LineageEdge> edges, String version) {
        List<LineageEdge> safe = edges == null ? Collections.emptyList() : edges;
        return new LineageGraph(safe, version);
    }

    public String getVersion() {
        return version;
    }

    public List<LineageEdge> getEdges() {
        return edges;
    }

    /**
     * 指定资产是否已有 from→to 边。
     */
    public boolean contains(String fromAssetId, String toAssetId) {
        return findEdge(fromAssetId, toAssetId).isPresent();
    }

    /**
     * 按 from→to 定位既有边（重复补录幂等判定依据）。
     */
    public Optional<LineageEdge> findEdge(String fromAssetId, String toAssetId) {
        List<LineageEdge> out = downstream.getOrDefault(fromAssetId, Collections.emptyList());
        return out.stream()
                .filter(e -> e.getToAssetId().equals(toAssetId))
                .findFirst();
    }

    /**
     * 环检测：判断补录 from→to 是否成环，成环返回从 to 出发到达 from 的
     * 既有边路径（冲突边为补录边 from→to，不在路径内）；无环返回空。
     *
     * <p>自环（from==to）返回空路径但视为成环（由调用方 {@link #ensureAcyclic} 判空）。</p>
     */
    public List<LineageEdge> findCyclePath(String fromAssetId, String toAssetId) {
        if (fromAssetId == null || toAssetId == null) {
            return Collections.emptyList();
        }
        // BFS：从 to 沿下游追踪，可达 from 即成环；parentEdge 记录进入节点的边用于回溯
        Map<String, LineageEdge> parentEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(toAssetId);
        visited.add(toAssetId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (LineageEdge edge : downstream.getOrDefault(current, Collections.emptyList())) {
                if (toAssetId.equals(edge.getToAssetId())) {
                    continue;
                }
                if (fromAssetId.equals(edge.getToAssetId())) {
                    return reconstructPath(parentEdge, edge, current, toAssetId);
                }
                if (!visited.contains(edge.getToAssetId())) {
                    visited.add(edge.getToAssetId());
                    parentEdge.put(edge.getToAssetId(), edge);
                    queue.add(edge.getToAssetId());
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 回溯 from 的路径：edge(current→from) + current 到 to 的入边链。
     */
    private List<LineageEdge> reconstructPath(Map<String, LineageEdge> parentEdge,
                                              LineageEdge lastEdge, String current, String toAssetId) {
        List<LineageEdge> path = new ArrayList<>();
        path.add(lastEdge);
        String node = current;
        while (!toAssetId.equals(node)) {
            LineageEdge parent = parentEdge.get(node);
            if (parent == null) {
                return path;
            }
            path.add(parent);
            node = parent.getFromAssetId();
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * 无环校验：补录边成环（含自环）抛 {@link LineageCycleException}（CYCLE，定位冲突边）。
     */
    public void ensureAcyclic(LineageEdge proposed) {
        if (proposed.getFromAssetId().equals(proposed.getToAssetId())) {
            throw new LineageCycleException(proposed, Collections.emptyList());
        }
        List<LineageEdge> cyclePath = findCyclePath(proposed.getFromAssetId(), proposed.getToAssetId());
        if (!cyclePath.isEmpty()) {
            throw new LineageCycleException(proposed, cyclePath);
        }
    }

    /**
     * 字段级无环校验：字段自环或闭环抛 {@link LineageCycleException}。
     */
    public void ensureColumnAcyclic(LineageEdge proposed) {
        if (proposed.getFromColumnId() == null || proposed.getToColumnId() == null) {
            ensureAcyclic(proposed);
            return;
        }

        if (proposed.getFromAssetId().equals(proposed.getToAssetId())
                && proposed.getFromColumnId().equalsIgnoreCase(proposed.getToColumnId())) {
            throw new LineageCycleException(proposed, Collections.emptyList());
        }

        List<LineageEdge> cyclePath = findColumnCyclePath(
                proposed.getFromAssetId(), proposed.getFromColumnId(),
                proposed.getToAssetId(), proposed.getToColumnId());
        if (!cyclePath.isEmpty()) {
            throw new LineageCycleException(proposed, cyclePath);
        }
    }

    /**
     * 字段级环检测路径搜索（从 toCol 出发沿下游搜索 fromCol）。
     */
    public List<LineageEdge> findColumnCyclePath(String fromAssetId, String fromColumnId,
                                                 String toAssetId, String toColumnId) {
        String targetKey = fromAssetId + ":" + fromColumnId.toLowerCase();
        String startKey = toAssetId + ":" + toColumnId.toLowerCase();

        Map<String, LineageEdge> parentEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startKey);
        visited.add(startKey);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (LineageEdge edge : edges) {
                if (edge.getFromColumnId() == null || edge.getToColumnId() == null) {
                    continue;
                }
                String edgeFromKey = edge.getFromAssetId() + ":" + edge.getFromColumnId().toLowerCase();
                String edgeToKey = edge.getToAssetId() + ":" + edge.getToColumnId().toLowerCase();

                if (edgeFromKey.equals(current)) {
                    if (edgeToKey.equals(targetKey)) {
                        return reconstructColumnPath(parentEdge, edge, current, startKey);
                    }
                    if (!visited.contains(edgeToKey)) {
                        visited.add(edgeToKey);
                        parentEdge.put(edgeToKey, edge);
                        queue.add(edgeToKey);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<LineageEdge> reconstructColumnPath(Map<String, LineageEdge> parentEdge,
                                                    LineageEdge lastEdge, String current, String startKey) {
        List<LineageEdge> path = new ArrayList<>();
        path.add(lastEdge);
        String node = current;
        while (!startKey.equals(node)) {
            LineageEdge parent = parentEdge.get(node);
            if (parent == null) {
                return path;
            }
            path.add(parent);
            node = parent.getFromAssetId() + ":" + parent.getFromColumnId().toLowerCase();
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * 字段级下游爆炸半径 (Blast Radius) BFS 遍历。
     *
     * @param startAssetId  源资产 ID
     * @param startColumnId 源字段 ID 或名称
     * @param maxDepth      最大遍历深度 (默认 5)
     * @return 按深度层级组织的受影响边列表 (Depth -> List<LineageEdge>)
     */
    public Map<Integer, List<LineageEdge>> findDownstreamColumnEdges(String startAssetId, String startColumnId, int maxDepth) {
        Map<Integer, List<LineageEdge>> result = new LinkedHashMap<>();
        int effectiveDepth = maxDepth <= 0 ? 5 : maxDepth;

        Set<String> visitedNodes = new HashSet<>();
        String rootKey = startAssetId + ":" + startColumnId.toLowerCase();
        visitedNodes.add(rootKey);

        Set<String> currentLevelNodes = new HashSet<>();
        currentLevelNodes.add(rootKey);

        for (int depth = 1; depth <= effectiveDepth && !currentLevelNodes.isEmpty(); depth++) {
            Set<String> nextLevelNodes = new HashSet<>();
            List<LineageEdge> depthEdges = new ArrayList<>();

            for (String nodeKey : currentLevelNodes) {
                for (LineageEdge edge : edges) {
                    if (edge.getFromColumnId() == null || edge.getToColumnId() == null) {
                        continue;
                    }
                    String edgeFromKey = edge.getFromAssetId() + ":" + edge.getFromColumnId().toLowerCase();
                    String edgeToKey = edge.getToAssetId() + ":" + edge.getToColumnId().toLowerCase();

                    if (edgeFromKey.equals(nodeKey)) {
                        depthEdges.add(edge);
                        if (!visitedNodes.contains(edgeToKey)) {
                            visitedNodes.add(edgeToKey);
                            nextLevelNodes.add(edgeToKey);
                        }
                    }
                }
            }

            if (!depthEdges.isEmpty()) {
                result.put(depth, Collections.unmodifiableList(depthEdges));
            }
            currentLevelNodes = nextLevelNodes;
        }

        return result;
    }

    /**
     * 图版本 token 乐观锁校验：token 提供且与当前版本不匹配抛
     * {@link LineageConflictException}（CONFLICT；恢复路径=重读图谱）。
     *
     * <p>token 缺省（null/空白）跳过校验（OpenAPI 字段可选，未参与并发保护的客户端放行）。</p>
     */
    public void ensureVersion(String graphVersionToken) {
        if (graphVersionToken != null && !graphVersionToken.trim().isEmpty()
                && (version == null || !version.equals(graphVersionToken.trim()))) {
            throw new LineageConflictException(
                    "图版本冲突（CONFLICT）：当前图版本已变化（" + version + "），"
                            + "请刷新血缘图谱获取最新 graphVersionToken 后重试");
        }
    }
}
