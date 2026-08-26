package com.yss.metadata.domain.governance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类传播异步任务（数据架构 PropagateTask：id/classification_id/version/status/coverage/operator/时间戳）。
 *
 * <p>幂等：同 classification+version 进行中任务复用（服务层实现），同版本只跑一次；
 * 状态流转 pending→running→success/failed；触发写 audit_log 审计；coverage 覆盖范围可核验。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropagateTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 触发源分类 id（沿其血缘传播） */
    private String classificationId;

    /** 传播版本（同版本只跑一次幂等键；由分类内容推导） */
    private String version;

    /** 状态（pending/running/success/failed） */
    private PropagateTaskStatus status;

    /** 覆盖范围（受影响资产数/明细，可核验） */
    private String coverage;

    /** 触发人（X-User-Id 解析值，缺省 default-user） */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;
}
