package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资产版本视图对象（详情聚合变更记录元素）。
 */
@Getter
@Setter
public class AssetVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 版本 id */
    private String id;

    /** 版本号 */
    private Integer version;

    /** schema 变更内容 */
    private String schemaDiff;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
