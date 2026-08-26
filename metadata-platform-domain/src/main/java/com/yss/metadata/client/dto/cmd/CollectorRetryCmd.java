package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * 失败重试 / 局部重采命令（冻结 OpenAPI POST /api/collectors/{id}/retry）。
 */
@Getter
@Setter
public class CollectorRetryCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 仅重采失败项（默认 true；实际局部重采物理逻辑 seam-deferred） */
    private Boolean failedItemsOnly = Boolean.TRUE;
}
