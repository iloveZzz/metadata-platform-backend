package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

/**
 * 凭据密文存储 / KMS 密文引用端口（Domain 定义，Infrastructure 实现）。
 *
 * <p>语义（SEC-05 / 数据架构 §5）：{@code agent_credential.credential_ref} 为 KMS 密文引用，
 * 永不落明文。本端口承载「明文 ↔ 密文引用」的双向变换：签发 / 轮换时以
 * {@link #reference} 生成密文引用落库；连接鉴权时以 {@link #dereference}
 * 解引用后做常量时间比较（由凭据校验实现承担）。</p>
 *
 * <p>实现约束（SEC-05/11）：明文仅在调用栈内存中存在，永不落库 / 日志 / 查询参数 /
 * 工具参数；实现不得输出任何含明文或密钥的日志。</p>
 *
 * <p><b>D3 人工评审点</b>：本端口为 KMS client seam。MVP 实现为本地密钥占位
 * （Infrastructure 的 {@code LocalCredentialCipher}，AES-256-GCM）；生产应替换为
 * 平台密钥管理 / 等价 KMS client（IC-04 归属确认），经同一端口接入不改领域层。</p>
 */
public interface CredentialCipher {

    /**
     * 将凭据明文加密为 KMS 密文引用（credential_ref，落库值）。
     *
     * @param plaintextSecret 传输期内的凭据明文（永不落库 / 日志）
     * @return 密文引用（非明文，可用于 {@code agent_credential.credential_ref} 列）
     */
    String reference(String plaintextSecret);

    /**
     * 将 KMS 密文引用解引用为凭据明文（仅供内存内常量时间比较，不落库 / 日志）。
     *
     * @param credentialRef 数据库存储的密文引用
     * @return 解引用后的凭据明文（调用方必须立即使用且不持久化）
     */
    String dereference(String credentialRef);
}
