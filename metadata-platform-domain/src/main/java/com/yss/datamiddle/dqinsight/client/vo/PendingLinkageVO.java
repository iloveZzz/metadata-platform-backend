package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 待关联队列条目（冻结 OpenAPI PendingLinkage；资产 ID 未命中，结果已入库，SB-05）。
 */
@Getter
@Setter
@NoArgsConstructor
public class PendingLinkageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联记录 ID（映射操作路径参数） */
    private String id;

    /** 未命中的资产 ID（资产不存在 / 名称变更 / 尚未入库） */
    private String assetId;

    /** 来源批次号 */
    private String batchNo;

    /** 来源工具 */
    private SourceTool sourceTool;

    /** 接收时间（ISO 8601） */
    private String receivedAt;

    /** 行数 */
    private Integer rowCount;

    /** 备注 */
    private String note;
}
