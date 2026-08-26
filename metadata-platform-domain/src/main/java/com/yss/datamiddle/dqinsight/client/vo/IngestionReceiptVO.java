package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 接入接收回执（冻结 OpenAPI IngestionReceipt，201）。
 */
@Getter
@Setter
@NoArgsConstructor
public class IngestionReceiptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 批次 ID */
    private String batchId;

    /** 批次号 */
    private String batchNo;

    /** 接入状态（已入库） */
    private IngestionStatus status;

    /** 关联状态（入库与关联解耦：linked / pending / none） */
    private LinkageState linkageStatus;

    /** 接收时间（ISO 8601） */
    private String receivedAt;

    /** 行数 */
    private Integer rowCount;
}
