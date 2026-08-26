package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 立即执行采集任务命令（冻结 OpenAPI POST /api/collectors/run）。
 */
@Getter
@Setter
public class CollectorRunCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 采集任务 id */
    @NotBlank(message = "采集任务 id 不能为空")
    private String collectorId;
}
