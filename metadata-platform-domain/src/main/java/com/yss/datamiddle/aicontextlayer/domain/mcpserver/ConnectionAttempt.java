package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Builder;
import lombok.Getter;

/**
 * 连接尝试（值对象）：携带传输期呈现凭据（Bearer Token）。
 *
 * <p>安全约束（SEC-05/11）：呈现凭据只在连接鉴权传输期内存在，永不落库、永不进日志、
 * 不随查询参数 / 工具参数传递；凭据仅经 Authorization header 进入（适配层保证）。</p>
 */
@Getter
@Builder
public class ConnectionAttempt {

    /** 呈现凭据（Bearer Token 原文）；缺失 / 空白按未携带凭据处理（禁匿名，SEC-05）。 */
    private final String presentedSecret;

    /**
     * 是否携带凭据。
     */
    public boolean hasCredential() {
        return presentedSecret != null && !presentedSecret.trim().isEmpty();
    }
}
