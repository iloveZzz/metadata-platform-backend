package com.yss.metadata.application.asset.service;

import com.yss.metadata.client.vo.AssetVO;

import java.util.List;

/**
 * 资产操作用例应用服务（WU-02-02）。
 *
 * <p>收藏幂等切换、认领 owner 唯一（409 语义）、标签覆盖式更新、
 * 归档-取消归档只读状态机（409 语义）；事务边界为单聚合事务。</p>
 */
public interface AssetActionService {

    /**
     * 收藏 / 取消收藏（幂等切换；返回最新收藏状态）。
     *
     * @param currentUserId 当前用户（RBAC slice 06 前 seam）
     */
    AssetVO toggleFavorite(String id, String currentUserId);

    /**
     * 认领 owner（已被他人认领抛认领冲突，409 语义；本人重复认领幂等）。
     */
    AssetVO claim(String id, String currentUserId);

    /**
     * 编辑标签（覆盖式全量替换；归档/已删除资产抛状态冲突，409 语义）。
     */
    AssetVO updateTags(String id, List<String> tags);

    /**
     * 归档（待认领/已认领 → 已归档只读；重复归档/已删除抛状态冲突，409 语义）。
     */
    AssetVO archive(String id);

    /**
     * 取消归档（已归档 → 恢复可编辑；非归档状态幂等）。
     */
    AssetVO unarchive(String id);

    /**
     * 剔除/软删除资产（标记 isExcluded = true）。
     */
    AssetVO exclude(String id);

    /**
     * 恢复已剔除资产（标记 isExcluded = false）。
     */
    AssetVO recover(String id);
}
