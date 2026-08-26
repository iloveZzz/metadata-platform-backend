package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类传播异步任务视图对象（冻结 OpenAPI POST /api/classifications/{id}/propagate
 * 202 响应 data；幂等复用与覆盖范围核验依据）。
 */
@Getter
@Setter
public class PropagateTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 id */
    private String id;

    /** 触发源分类 id */
    private String classificationId;

    /** 传播版本（同版本只跑一次幂等键） */
    private String version;

    /** 状态（pending/running/success/failed） */
    private String status;

    /** 覆盖范围（受影响资产数/明细，可核验） */
    private String coverage;

    /** 触发人 */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;
}
