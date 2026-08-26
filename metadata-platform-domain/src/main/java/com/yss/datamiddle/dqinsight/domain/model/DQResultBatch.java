package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * DQ 结果批次聚合根（DQResultBatch）。
 *
 * <p>(sourceTool, batchNo) 为幂等去重键（SB-09 MVP 行为基线）；解析成功即入库（与资产关联解耦，SB-05）；
 * validUntil = executionTime + 30 天（OQ-03 已确认默认窗口，配置化 P1）。</p>
 */
@Getter
@Setter
public class DQResultBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 有效期窗口（天，OQ-03 默认 30 天） */
    public static final int VALIDITY_WINDOW_DAYS = 30;

    /** 主键（保存后由持久化分配） */
    private Long id;

    /** 批次号（幂等去重键之一） */
    private String batchNo;

    /** 来源工具（幂等去重键之一） */
    private SourceTool sourceTool;

    /** 格式类型 */
    private FormatType formatType;

    /** 接入通道 ID（可空；API 推送通道认证后绑定） */
    private String channelId;

    /** 工具执行时间 = 结果时间（有效期起算） */
    private Instant executionTime;

    /** 接收时间 */
    private Instant receivedAt;

    /** 行数 */
    private int rowCount;

    /** 接入状态 */
    private IngestionStatus status;

    /** 关联状态 */
    private LinkageState linkageStatus;

    /** 错误分类（parse-failed 时） */
    private ErrorCategory errorCategory;

    /** 错误信息（脱敏，不泄露凭证） */
    private String errorMessage;

    /** 结果有效期至（结果时间 + 有效期窗口）；已失效时早于当前时间 */
    private Instant validUntil;

    private DQResultBatch() {
    }

    /**
     * 持久化 / 映射专用构造（仅 MapStruct 反向映射 toDomain 使用；字段经 setter 回填；
     * 业务创建必须使用 createIngested / createParseFailed 工厂）。
     */
    public static DQResultBatch forPersistenceLoad() {
        return new DQResultBatch();
    }

    /**
     * 解析成功即入库：创建已入库批次（status = ingested）。
     *
     * @param batchNo       批次号（必填）
     * @param sourceTool    来源工具
     * @param formatType    格式类型
     * @param channelId     接入通道 ID（可空）
     * @param executionTime 工具执行时间（必填）
     * @param rows          规则明细
     * @return 已入库批次
     */
    public static DQResultBatch createIngested(String batchNo, SourceTool sourceTool, FormatType formatType,
            String channelId, Instant executionTime, List<RuleResultRow> rows) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new IllegalArgumentException("batchNo 必填");
        }
        if (executionTime == null) {
            throw new IllegalArgumentException("executionTime 必填");
        }
        DQResultBatch batch = new DQResultBatch();
        batch.batchNo = batchNo.trim();
        batch.sourceTool = sourceTool;
        batch.formatType = formatType;
        batch.channelId = channelId;
        batch.executionTime = executionTime;
        batch.receivedAt = Instant.now();
        batch.rowCount = rows == null ? 0 : rows.size();
        batch.status = IngestionStatus.INGESTED;
        batch.linkageStatus = LinkageState.NONE;
        batch.validUntil = executionTime.plus(VALIDITY_WINDOW_DAYS, ChronoUnit.DAYS);
        return batch;
    }

    /**
     * 创建解析失败批次（status = parse-failed），用于 422 时落 dq_batch 供接入记录查询。
     *
     * @param formatType    格式类型
     * @param sourceTool    来源工具（不可恢复时为 null）
     * @param batchNo       批次号（不可恢复时为 null，缺省由平台生成）
     * @param channelId     接入通道 ID（可空）
     * @param errorCategory 错误分类（format / auth / network）
     * @param errorMessage  脱敏错误信息
     * @return 解析失败批次
     */
    public static DQResultBatch createParseFailed(FormatType formatType, SourceTool sourceTool, String batchNo,
            String channelId, ErrorCategory errorCategory, String errorMessage) {
        DQResultBatch batch = new DQResultBatch();
        batch.batchNo = (batchNo == null || batchNo.trim().isEmpty())
                ? generateBatchNo(formatType) : batchNo.trim();
        batch.sourceTool = sourceTool != null ? sourceTool : SourceTool.GENERIC;
        batch.formatType = formatType;
        batch.channelId = channelId;
        batch.executionTime = null;
        batch.receivedAt = Instant.now();
        batch.rowCount = 0;
        batch.status = IngestionStatus.PARSE_FAILED;
        batch.linkageStatus = LinkageState.NONE;
        batch.errorCategory = errorCategory;
        batch.errorMessage = errorMessage;
        batch.validUntil = null;
        return batch;
    }

    /**
     * 批次行数上限校验（SB-10 已确认 ≤5 万条 / 批次；超限 413 err.dq.batch.too-large，M3 决策）。
     *
     * @param maxRowsPerBatch 单批次上限（50000）
     */
    public void validateRowCount(int maxRowsPerBatch) {
        if (rowCount > maxRowsPerBatch) {
            throw new com.yss.datamiddle.dqinsight.domain.exception.BatchTooLargeException(rowCount, maxRowsPerBatch);
        }
    }

    public void assignId(Long id) {
        this.id = id;
    }

    /**
     * 平台生成批次号（CSV batch_no 缺省时）。
     */
    public static String generatePlatformBatchNo(FormatType formatType) {
        String prefix = formatType == null ? "plat" : formatType.getCode();
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String generateBatchNo(FormatType formatType) {
        return generatePlatformBatchNo(formatType);
    }
}
