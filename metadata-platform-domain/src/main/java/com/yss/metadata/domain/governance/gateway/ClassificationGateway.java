package com.yss.metadata.domain.governance.gateway;

import com.yss.metadata.domain.governance.model.Classification;

import java.util.List;
import java.util.Optional;

/**
 * 分级分类结果仓储端口（治理域；Domain 定义，Infrastructure 实现）。
 *
 * <p>classification 表：候选结果列表、确认/修正保存与传播源解析
 * （列级分类 → 所属资产 id）。</p>
 */
public interface ClassificationGateway {

    /**
     * 查询全部分类结果（候选/已确认/已修正，按 id 序；无创建时间列，排序稳定即可）。
     */
    List<Classification> findAll();

    /**
     * 按 id 查询分类结果。
     */
    Optional<Classification> findById(String id);

    /**
     * 保存分类结果（新增或更新状态/分类名）。
     */
    Classification save(Classification classification);

    /**
     * 保存识别候选（幂等：同 asset_id+column_id+name 已存在则跳过）。
     *
     * @return 是否实际新增（幂等跳过返回 false；供调用方统计新增候选数）
     */
    boolean saveCandidate(Classification candidate);

    /**
     * 解析分类所属资产 id（列级分类经 asset_column 反查父资产）。
     */
    Optional<String> resolveSourceAssetId(Classification classification);
}
