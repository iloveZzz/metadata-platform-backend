package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentCredential;

import java.util.List;
import java.util.Optional;

public interface AgentCredentialGateway{
    /**
     * Add AgentCredential
     *
     * @param entity entity
     * @return id
     */
    String addAgentCredential(AgentCredential entity);
    /**
     * Update AgentCredential
     *
     * @param entity entity
     * @return boolean
     */
    boolean updateAgentCredential(AgentCredential entity);
    /**
     * Delete AgentCredential
     *
     * @param id id
     * @return boolean
     */
    boolean deleteAgentCredential(String id);
    /**
     * Get AgentCredential by id
     *
     * @param id id
     * @return optional
     */
    Optional<AgentCredential> getAgentCredentialById(String id);
    /**
      * Page AgentCredential
      *
      * @param query query
      * @return page result
      */
    PageResult<AgentCredential> pageAgentCredential(PageQuery query);

    /**
     * 列出全部凭据行（含 REVOKED / ROTATED / EXPIRED 状态）。
     *
     * <p>凭据校验路径（WU03）：credential_ref 为 KMS 密文引用，无法按明文索引查询，
     * 需全量候选解引用后常量时间比较；MVP 规模（数据架构 §9，Agent ≤50）下全量扫描可接受。
     * 返回含 REVOKED 行供吊销即时生效联动（SEC-05），不得过滤状态。</p>
     *
     * @return 全部凭据行（无过滤）
     */
    List<AgentCredential> listCredentials();
}
