package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;

import java.util.Optional;

/**
 * 凭据校验端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>实现约束（SEC-05 / 数据架构 §5）：凭据密文存储（KMS 密文引用 credential_ref），
 * 校验按密文引用解引用后进行常量时间比较；无效 / 不存在的凭据返回 {@link Optional#empty()}，
 * 已吊销凭据返回 REVOKED 状态主体（供吊销即时生效联动，不返回 empty）。</p>
 */
public interface CredentialVerificationGateway {

    /**
     * 校验呈现凭据并返回对应凭据主体。
     *
     * @param presentedSecret 连接传输期内呈现的凭据原文（永不落库 / 日志）
     * @return 校验成功返回凭据主体；无效 / 不存在返回 {@link Optional#empty()}
     */
    Optional<AgentCredential> verify(String presentedSecret);
}
