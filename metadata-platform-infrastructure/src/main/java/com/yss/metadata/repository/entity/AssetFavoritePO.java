package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏持久化对象（数据架构 asset_favorite 表；切片 02 新增）。
 *
 * <p>复合主键 (asset_id, user_id)（无单列主键，MyBatis-Plus 以 Wrapper 完成
 * 读写，不依赖 selectById）；收藏幂等切换、我的资产/仅看收藏筛选依据。</p>
 */
@Getter
@Setter
@TableName("asset_favorite")
public class AssetFavoritePO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 id（复合主键） */
    @TableField("asset_id")
    private String assetId;

    /** 用户 id（复合主键；RBAC slice 06 前为 X-User-Id 解析值或 default-user） */
    @TableField("user_id")
    private String userId;

    /** 收藏时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
