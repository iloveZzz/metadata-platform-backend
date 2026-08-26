package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 资产详情聚合视图对象（冻结 OpenAPI GET /api/assets/{id} 响应 data）。
 *
 * <p>在 {@link AssetVO} 基础上聚合字段清单、版本/变更记录、标签与
 * 当前用户收藏状态；血缘/影响分析 tab 属切片 03。</p>
 */
@Getter
@Setter
public class AssetDetailVO extends AssetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签列表 */
    private List<String> tags;

    /** 字段清单 */
    private List<AssetColumnVO> columns;

    /** 版本/变更记录（最新在前） */
    private List<AssetVersionVO> versions;
}
