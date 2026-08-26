package com.yss.metadata.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 影响分析递归 CTE 查询结果持久化对象（下游命中 + 资产组合字段）。
 *
 * <p>列别名与属性驼峰同名（assetId/name/type/domain/classification/depth），
 * 兼容生产 map-underscore-to-camel-case 与 H2 测试（无下划线直映）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactHitPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下游资产 id */
    private String assetId;

    /** 资产名称（asset.name） */
    private String name;

    /** 资产类型 */
    private String type;

    /** 数据域 */
    private String domain;

    /** 分级分类 */
    private String classification;

    /** 影响深度 */
    private Integer depth;
}
