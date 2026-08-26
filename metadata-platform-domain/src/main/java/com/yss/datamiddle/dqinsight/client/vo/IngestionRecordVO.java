package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 接入记录（冻结 OpenAPI IngestionRecord，通道管理页接入记录）。
 */
@Getter
@Setter
@NoArgsConstructor
public class IngestionRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 批次 ID */
    private String batchId;

    /** 批次号 */
    private String batchNo;

    /** 来源工具 */
    private SourceTool sourceTool;

    /** 格式类型 */
    private FormatType formatType;

    /** 接入状态 */
    private IngestionStatus status;

    /** 关联状态 */
    private LinkageState linkageStatus;

    /** 接入通道 ID */
    private String channelId;

    /** 接收时间（ISO 8601） */
    private String receivedAt;

    /** 工具执行时间（ISO 8601） */
    private String executionTime;

    /** 行数 */
    private Integer rowCount;

    /** 错误分类 */
    private ErrorCategory errorCategory;

    /** 解析失败原因 / 错误说明（含 CSV 行号等定位信息，脱敏） */
    private String errorMessage;

    /** 结果有效期至（ISO 8601）；已失效时早于当前时间 */
    private String validUntil;
}
