package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 血缘图谱视图对象（冻结 OpenAPI GET /api/assets/{id}/lineage 响应 data）。
 *
 * <p>空血缘以空 edges 表达（非错误）；graphVersionToken 供人工补录
 * 乐观锁使用（重读图谱获取最新 token 为 CONFLICT 恢复路径）。</p>
 */
@Getter
@Setter
public class LineageGraphVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 边列表（from=资产 或 to=资产的邻域边；confidence 筛选后） */
    private List<LineageEdgeVO> edges;

    /** 图版本 token（当前最新；空图可能为 null） */
    private String graphVersionToken;
}
