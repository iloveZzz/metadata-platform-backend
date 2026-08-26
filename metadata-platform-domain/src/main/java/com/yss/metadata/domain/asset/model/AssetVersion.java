package com.yss.metadata.domain.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资产版本值对象（数据架构 asset_version 表；详情聚合变更记录）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 版本主键（UUID） */
    private String id;

    /** 版本号（资产内递增） */
    private Integer version;

    /** schema 变更内容 */
    private String schemaDiff;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
