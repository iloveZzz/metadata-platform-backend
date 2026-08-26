package com.yss.metadata.domain.lineage.gateway;

import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageGraph;

/**
 * 血缘图谱仓储端口（血缘域；Domain 定义，Infrastructure 实现）。
 *
 * <p>邻接表持久化：全量图加载（环检测/版本校验用）、按资产邻域查询
 * （图谱展示，confidence 筛选，空血缘空结构）、补录边写入。
 * 图版本 token 由边表 graph_version 最大值推导（乐观锁）。</p>
 */
public interface LineageGraphRepository {

    /**
     * 加载全量血缘图（环检测与图版本校验用；含全部边 + 最新图版本）。
     */
    LineageGraph loadGraph();

    /**
     * 按资产查询血缘邻域（from=资产 或 to=资产），confidence 非空时筛选；
     * 空血缘返回空边结构（非错误）。
     */
    LineageGraph findGraph(String assetId, LineageConfidence confidence);

    /**
     * 保存血缘边（新增；id 为空时由实现生成；graphVersion 由应用层指定）。
     */
    LineageEdge save(LineageEdge edge);

    /**
     * 按 ID 删除血缘边。
     */
    void deleteById(String edgeId);
}
