package com.yss.datamiddle.aicontextlayer.config;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.SessionManager;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentCredentialGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AuditLogGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialCipher;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.CredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.McpSessionGateway;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.SessionRepository;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.ToolRegistryGateway;
import com.yss.datamiddle.aicontextlayer.infrastructure.client.BaseUrlWhitelistValidator;
import com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver.JdbcCredentialVerificationGateway;
import com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver.JdbcSessionRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.mcpserver.LocalCredentialCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 装配（WU-01-01 骨架；WU-01-03/04 将 InMemory seam 替换为 DB-backed 实现；WU-01-05 装配工具白名单与拦截器）。
 *
 * <p><b>seam 替换说明（WU-01-03/04）</b>：InMemory 凭据校验 / 会话存储 seam-deferred 实现
 * 已删除并替换为真实 DB 实现——凭据校验 {@link JdbcCredentialVerificationGateway}
 * （读取 agent_credential 表 + {@link CredentialCipher} 密文引用解引用 + 常量时间比较，SEC-05 / D3），
 * 会话存储 {@link JdbcSessionRepository}（mcp_session 表持久化 / 单聚合事务 / 回收 / 强制断开，
 * WU04）。本配置不再装配任何 InMemory 端口（无冒充生产持久化）。</p>
 *
 * <p><b>D3 人工评审点</b>：凭据密文存储当前为本地密钥占位实现
 * {@link LocalCredentialCipher}（AES-256-GCM，密钥来自 {@code acl.security.credential.local-cipher-key}，
 * Base64 32 字节，缺失即 fail-fast）；生产应替换为真实 KMS client seam（IC-04 归属确认）。
 * 本配置不 seed 任何明文凭据（SEC-05）。</p>
 *
 * <p>MCP SDK transport seam 保持（D4 版本锁定后接入，合同 seam_deferred）。</p>
 */
@Configuration
public class McpServerConfiguration {

    @Bean
    public CredentialCipher credentialCipher(
            @Value("${acl.security.credential.local-cipher-key:}") String localCipherKey) {
        return new LocalCredentialCipher(localCipherKey);
    }

    @Bean
    public CredentialVerificationGateway credentialVerificationGateway(
            AgentCredentialGateway agentCredentialGateway,
            CredentialCipher credentialCipher) {
        return new JdbcCredentialVerificationGateway(agentCredentialGateway, credentialCipher);
    }

    @Bean
    public SessionRepository sessionRepository(McpSessionGateway mcpSessionGateway) {
        return new JdbcSessionRepository(mcpSessionGateway);
    }

    @Bean
    public SessionManager sessionManager(SessionRepository sessionRepository) {
        return new SessionManager(sessionRepository);
    }

    @Bean
    public ConnectionAuthenticator connectionAuthenticator(
            CredentialVerificationGateway credentialVerificationGateway,
            SessionManager sessionManager,
            @org.springframework.beans.factory.annotation.Qualifier("aclAuditLogGatewayImpl") AuditLogGateway auditLogGateway) {
        return new ConnectionAuthenticator(credentialVerificationGateway, sessionManager, auditLogGateway);
    }

    @Bean
    public ReadOnlyToolRegistry readOnlyToolRegistry(ToolRegistryGateway toolRegistryGateway) {
        return new ReadOnlyToolRegistry(toolRegistryGateway);
    }

    @Bean
    public BaseUrlWhitelistValidator baseUrlWhitelistValidator() {
        return new BaseUrlWhitelistValidator();
    }
}

