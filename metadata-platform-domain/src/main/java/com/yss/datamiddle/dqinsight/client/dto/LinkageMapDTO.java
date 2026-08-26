package com.yss.datamiddle.dqinsight.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 人工映射请求（冻结 OpenAPI LinkageMapRequest：assetId 必填；
 * confirmOverwrite 目标批次已关联时覆盖需二次确认，409 时重试携带）。
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkageMapDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标主平台资产 ID（防腐层消费 GET /api/assets 校验存在） */
    private String assetId;

    /** 已关联覆盖二次确认（默认 false） */
    private boolean confirmOverwrite = false;
}
