package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Agent 凭据领域模型（SEC-05，契约第 7 节）。
 *
 * <p>凭据密文存储：本对象只承载 KMS 密文引用 {@code credentialRef}（非本地明文），
 * 呈现凭据（Bearer Token）仅在连接鉴权传输期内存在，永不落库、永不进日志（SEC-05/11）。</p>
 *
 * <p>实现 {@link Serializable}：作为持久层 Gateway 分页返回类型（{@code PageResult<T extends Serializable>}）要求。</p>
 */
@Getter
@Builder
public class AgentCredential implements Serializable {

    private static final long serialVersionUID = 1L;


    /** 凭据主体（Agent 身份标识）。 */
    private final String agentId;

    /** 凭据版本（会话绑定版本；吊销 / 轮换按版本标记）。 */
    private final String credentialVersion;

    /** KMS / 平台密钥管理密文引用，不存明文（数据架构 §5）。 */
    private final String credentialRef;

    /** 凭据状态（ACTIVE / REVOKED / ROTATED / EXPIRED）。 */
    private final CredentialStatus status;

    private final Instant issuedAt;

    private final Instant expiresAt;

    private final Instant revokedAt;

    /**
     * 凭据是否已吊销（吊销即时生效，SEC-05）。
     */
    public boolean isRevoked() {
        return status == CredentialStatus.REVOKED;
    }

    /**
     * 凭据在指定时刻是否可用（生效中且未过期）。
     */
    public boolean isUsableAt(Instant now) {
        if (status != CredentialStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || !expiresAt.isBefore(now);
    }
}
