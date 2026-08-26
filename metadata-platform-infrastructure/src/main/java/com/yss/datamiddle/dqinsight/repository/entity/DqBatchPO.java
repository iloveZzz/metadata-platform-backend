package com.yss.datamiddle.dqinsight.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DQ 结果批次持久化对象（dq_batch；UNIQUE(source_tool, batch_no) 幂等去重键，SB-09）。
 */
@Getter
@Setter
@TableName("dq_batch")
public class DqBatchPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次号 */
    @TableField("batch_no")
    private String batchNo;

    /** 来源工具 */
    @TableField("source_tool")
    private String sourceTool;

    /** 格式类型 */
    @TableField("format_type")
    private String formatType;

    /** 接入通道 ID（可空） */
    @TableField("channel_id")
    private String channelId;

    /** 接入状态（ingested / parse-failed / invalidated） */
    @TableField("status")
    private String status;

    /** 关联状态（linked / pending / none） */
    @TableField("linkage_status")
    private String linkageStatus;

    /** 接收时间 */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /** 工具执行时间（结果时间，有效期起算） */
    @TableField("execution_time")
    private LocalDateTime executionTime;

    /** 行数 */
    @TableField("row_count")
    private Integer rowCount;

    /** 错误分类（parse-failed 时） */
    @TableField("error_category")
    private String errorCategory;

    /** 错误信息（脱敏） */
    @TableField("error_message")
    private String errorMessage;

    /** 结果有效期至（结果时间 + 30 天） */
    @TableField("valid_until")
    private LocalDateTime validUntil;
}
