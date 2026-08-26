package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AuditLogGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 连接鉴权领域服务（契约第 7 节 SEC-05，数据架构 §6.1 鉴权失败路径）。
 *
 * <p>状态机：凭据缺失（禁匿名）/ 无效 / 过期 / 已吊销 → 统一拒绝连接
 * （{@link McpErrorCode#unauthorized}），会话不建立；已吊销凭据额外触发活跃会话
 * 强制断开（吊销即时生效，SEC-05）；通过校验后建立会话并绑定凭据版本。</p>
 *
 * <p>失败留痕（SEC-06，WU03 接线）：四种鉴权失败均<strong>同步</strong>写入
 * {@code audit_log}（调用即写，数据架构 §6.1 鉴权失败路径——同步写入、失败留痕不可丢失；
 * 禁止以异步 @AuditLog 注解替代）。审计行不含任何凭据明文（SEC-05/11）。若审计写入失败，
 * 不静默吞掉：按数据架构 §6.1 映射 {@link McpErrorCode#internal_error} 并保留底层原因。</p>
 */
public class ConnectionAuthenticator {

    /** 鉴权失败审计连接级工具标记（数据架构 §6.1：tool = 连接级标记）。 */
    static final String CONNECTION_LEVEL_TOOL = "connection";

    /** 鉴权失败审计会话占位（会话不建立，session_id NOT NULL 列以占位填充）。 */
    static final String NO_SESSION_MARKER = "no-session";

    /** 无法识别主体的鉴权失败审计主体占位（凭据无效 / 缺失时 agent_id 未知）。 */
    static final String UNKNOWN_AGENT_MARKER = "unknown";

    private final CredentialVerificationGateway credentialVerificationGateway;
    private final SessionManager sessionManager;
    private final AuditLogGateway auditLogGateway;

    public ConnectionAuthenticator(CredentialVerificationGateway credentialVerificationGateway,
                                   SessionManager sessionManager,
                                   AuditLogGateway auditLogGateway) {
        this.credentialVerificationGateway = credentialVerificationGateway;
        this.sessionManager = sessionManager;
        this.auditLogGateway = auditLogGateway;
    }

    /**
     * 鉴权连接尝试：凭据校验 + 吊销检查 + 会话建立。
     *
     * @param attempt 连接尝试（携带传输期呈现凭据）
     * @param now     当前时刻
     * @return 建立的会话（绑定 Agent 身份与凭据版本）
     * @throws McpException 凭据缺失 / 无效 / 过期 / 已吊销 → {@link McpErrorCode#unauthorized}；
     *                      审计写入失败 → {@link McpErrorCode#internal_error}
     */
    public McpSession authenticate(ConnectionAttempt attempt, Instant now) {
        // 禁匿名（SEC-05）：凭据缺失 → 统一 unauthorized + 失败留痕（SEC-06）
        if (attempt == null || !attempt.hasCredential()) {
            recordAuthFailure(null, now);
            throw McpException.of(McpErrorCode.UNAUTHORIZED);
        }
        Optional<AgentCredential> verified =
            credentialVerificationGateway.verify(attempt.getPresentedSecret());
        // 凭据无效 / 不存在 → 统一 unauthorized（不区分具体原因，SEC-05）+ 失败留痕
        if (!verified.isPresent()) {
            recordAuthFailure(null, now);
            throw McpException.of(McpErrorCode.UNAUTHORIZED);
        }
        AgentCredential credential = verified.get();
        // 吊销即时生效（SEC-05）：已吊销凭据强制断开其活跃会话并拒绝连接 + 失败留痕
        if (credential.isRevoked()) {
            sessionManager.forceTerminateByCredential(
                credential.getAgentId(), credential.getCredentialVersion());
            recordAuthFailure(credential, now);
            throw McpException.of(McpErrorCode.UNAUTHORIZED);
        }
        // 过期 / 非生效状态 → 统一 unauthorized + 失败留痕
        if (!credential.isUsableAt(now)) {
            recordAuthFailure(credential, now);
            throw McpException.of(McpErrorCode.UNAUTHORIZED);
        }
        return sessionManager.establish(credential, now);
    }

    /**
     * 同步写入鉴权失败审计（SEC-06，调用即写；数据架构 §6.1 鉴权失败路径）。
     *
     * <p>审计内容：连接级 tool 标记、result_code = unauthorized、mcp_request_id 每次失败唯一；
     * 不含凭据明文 / 堆栈 / 内部字段名（SEC-05/11）。主体标识在可识别时取凭据 agent_id，
     * 无效 / 缺失凭据无法识别主体时以 {@link #UNKNOWN_AGENT_MARKER} 占位。</p>
     *
     * @param credential 可识别的凭据主体；无效 / 缺失时为 {@code null}
     * @param now        失败时刻
     * @throws McpException 审计写入失败 → {@link McpErrorCode#internal_error}（§6.1：不静默吞掉）
     */
    private void recordAuthFailure(AgentCredential credential, Instant now) {
        AuditLog audit = new AuditLog();
        audit.setMcpRequestId(UUID.randomUUID().toString());
        audit.setSessionId(NO_SESSION_MARKER);
        audit.setAgentId(credential == null ? UNKNOWN_AGENT_MARKER : credential.getAgentId());
        audit.setTool(CONNECTION_LEVEL_TOOL);
        audit.setResultCode(McpErrorCode.UNAUTHORIZED.getCode());
        audit.setTimestamp(LocalDateTime.now());
        // internal_permission_flag 为资产访问权限判定标记（403/404/越权/域外剔除，SEC-03），
        // 连接级鉴权失败不适用 → 留空（仅内部可见字段，不进入 MCP 响应）
        try {
            auditLogGateway.addAuditLog(audit);
        } catch (RuntimeException e) {
            // 审计不可用即受信保证不成立（数据架构 §6.1）：映射 internal_error 并保留底层原因
            throw McpException.of(McpErrorCode.INTERNAL_ERROR, e);
        }
    }
}
