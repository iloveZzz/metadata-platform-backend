package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.ImpactHitPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 影响分析查询 Mapper（原生 SQL 递归 CTE；人工确认项，收敛于本基础设施端口）。
 *
 * <p>语义：以 #{assetId} 为起点沿 lineage_edge.from_asset → to_asset 下游全量召回；
 * 环保护（路径内边 id 去重，POSITION 判定）+ 深度上限（#{maxDepth}）；
 * LEFT JOIN asset 填充名称/类型/数据域/分类。递归 CTE 为 MySQL 8 / H2 2.x 通用语法。</p>
 */
public interface LineageImpactMapper extends BasePlusRepository<ImpactHitPO> {

    @Select("WITH RECURSIVE downstream(to_asset, depth, path) AS (\n"
            + "  SELECT e.to_asset, 1, CAST('|' AS CHAR(2000))\n"
            + "  FROM lineage_edge e\n"
            + "  WHERE e.from_asset = #{assetId}\n"
            + "  UNION ALL\n"
            + "  SELECT e.to_asset, d.depth + 1, CONCAT(d.path, e.id, '|')\n"
            + "  FROM downstream d\n"
            + "  JOIN lineage_edge e ON e.from_asset = d.to_asset\n"
            + "  WHERE d.depth < #{maxDepth}\n"
            + "    AND POSITION(CONCAT('|', e.id, '|') IN d.path) = 0\n"
            + ")\n"
            + "SELECT d.to_asset AS assetId, a.name AS name, a.type AS type,\n"
            + "       a.domain AS domain, a.classification AS classification, d.depth AS depth\n"
            + "FROM downstream d\n"
            + "LEFT JOIN asset a ON a.id = d.to_asset\n")
    List<ImpactHitPO> selectDownstream(@Param("assetId") String assetId, @Param("maxDepth") int maxDepth);
}
