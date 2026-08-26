package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP 错误码全集（冻结契约第 10.1 节，决策 D-05 共 9 个错误码）。
 *
 * <p>枚举常量命名遵循 Java 常量规范（UPPER_SNAKE）；对外 wire 码为小写字面值
 * （{@code code}，与契约表逐项一致，如 {@code "unauthorized"}）。可重试语义与契约表
 * 「可重试」列逐项一致：unauthorized / invalid_params / upstream_timeout /
 * upstream_unavailable / rate_limited / internal_error 可重试；
 * tool_not_found / asset_not_found / upstream_too_large 不可重试。</p>
 */
public enum McpErrorCode {

    /** 连接鉴权失败（凭据缺失 / 无效 / 过期 / 已吊销）；会话不建立（SEC-05）。 */
    UNAUTHORIZED("unauthorized", true, "连接鉴权失败：凭据缺失 / 无效 / 过期 / 已吊销，会话不建立"),
    /** 未注册工具 / 写类工具尝试；方法不存在或拒绝（SEC-09）。 */
    TOOL_NOT_FOUND("tool_not_found", false, "未注册工具或写类工具尝试，方法不存在"),
    /** MCP 层参数校验失败（对齐主平台 422 语义）。 */
    INVALID_PARAMS("invalid_params", true, "参数校验失败"),
    /** 单资产不存在或无权（403/404 统一隐藏，SEC-03）。 */
    ASSET_NOT_FOUND("asset_not_found", false, "资产不存在或无权访问"),
    /** 主平台响应超时（对齐 err.network.timeout 方向）。 */
    UPSTREAM_TIMEOUT("upstream_timeout", true, "上游服务响应超时"),
    /** 主平台不可用（网络 / 5xx / 熔断）。 */
    UPSTREAM_UNAVAILABLE("upstream_unavailable", true, "上游服务不可用"),
    /** 血缘 / 影响图超过 MCP 层规模上限（SEC-07）。 */
    UPSTREAM_TOO_LARGE("upstream_too_large", false, "上游结果超过规模上限"),
    /** 触发 MCP 层限流（429 语义，SEC-07）。 */
    RATE_LIMITED("rate_limited", true, "请求过于频繁，已限流"),
    /** 服务端内部错误；响应不含堆栈 / 内部字段名（SEC-11）。 */
    INTERNAL_ERROR("internal_error", true, "服务端内部错误");

    private static final Map<String, McpErrorCode> BY_CODE = Arrays.stream(values())
        .collect(Collectors.toMap(McpErrorCode::getCode, Function.identity()));

    private final String code;
    private final boolean retryable;
    private final String description;

    McpErrorCode(String code, boolean retryable, String description) {
        this.code = code;
        this.retryable = retryable;
        this.description = description;
    }

    /**
     * 错误码 wire 字面值（冻结契约第 10.1 节，如 {@code "unauthorized"}）。
     */
    public String getCode() {
        return code;
    }

    /**
     * 是否可重试（契约「可重试」列；可重试时调用方应退避重试）。
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 清洁错误描述（对外响应消息来源；不含堆栈、内部字段名、内部配置、凭据，SEC-11）。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 按错误码 wire 字面值反查枚举。
     *
     * @param code 错误码字面值（如 {@code "unauthorized"}）
     * @return 匹配的枚举；未知错误码返回 {@link Optional#empty()}
     */
    public static Optional<McpErrorCode> fromCode(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
