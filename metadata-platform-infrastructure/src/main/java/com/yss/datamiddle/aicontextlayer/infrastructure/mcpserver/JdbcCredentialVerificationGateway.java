package com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentCredentialGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * 凭据校验端口的 DB-backed 实现（WU03，替换 InMemory seam-deferred 实现）。
 *
 * <p>读取 {@code agent_credential} 表（经 {@link AgentCredentialGateway#listCredentials}，
 * 含 REVOKED / ROTATED / EXPIRED 行——吊销即时生效需能识别已吊销主体，SEC-05），
 * 对每条候选凭据经 {@link CredentialCipher#dereference} 解引用 KMS 密文引用后做
 * <b>常量时间比较</b>（{@link MessageDigest#isEqual}，SEC-05）。</p>
 *
 * <p>凭据明文仅在 {@code verify} 调用栈内存中存在，永不落库 / 日志（SEC-05/11）；
 * 候选凭据全量扫描为 MVP 规模（数据架构 §9，Agent ≤50）下的实现取舍。</p>
 */
public class JdbcCredentialVerificationGateway implements CredentialVerificationGateway {

    private final AgentCredentialGateway agentCredentialGateway;
    private final CredentialCipher credentialCipher;

    public JdbcCredentialVerificationGateway(AgentCredentialGateway agentCredentialGateway,
                                             CredentialCipher credentialCipher) {
        this.agentCredentialGateway = agentCredentialGateway;
        this.credentialCipher = credentialCipher;
    }

    @Override
    public Optional<AgentCredential> verify(String presentedSecret) {
        if (presentedSecret == null || presentedSecret.trim().isEmpty()) {
            return Optional.empty();
        }
        byte[] presentedBytes = presentedSecret.getBytes(StandardCharsets.UTF_8);
        for (AgentCredential credential : agentCredentialGateway.listCredentials()) {
            if (matches(presentedBytes, credential.getCredentialRef())) {
                return Optional.of(credential);
            }
        }
        return Optional.empty();
    }

    /**
     * 解引用候选凭据密文引用并与呈现凭据做常量时间比较。
     *
     * <p>解引用 / 格式异常（数据损坏）按「不匹配」处理（fail closed，不区分具体原因，
     * SEC-05 统一 unauthorized；异常不向外暴露）。</p>
     */
    private boolean matches(byte[] presentedBytes, String credentialRef) {
        if (credentialRef == null || credentialRef.isEmpty()) {
            return false;
        }
        try {
            String candidateSecret = credentialCipher.dereference(credentialRef);
            return MessageDigest.isEqual(presentedBytes,
                candidateSecret.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
