package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AgentDomain;

import java.util.Optional;

public interface AgentDomainGateway{
    /**
     * Add AgentDomain
     *
     * @param entity entity
     * @return id
     */
    String addAgentDomain(AgentDomain entity);
    /**
     * Update AgentDomain
     *
     * @param entity entity
     * @return boolean
     */
    boolean updateAgentDomain(AgentDomain entity);
    /**
     * Delete AgentDomain
     *
     * @param id id
     * @return boolean
     */
    boolean deleteAgentDomain(String id);
    /**
     * Get AgentDomain by id
     *
     * @param id id
     * @return optional
     */
    Optional<AgentDomain> getAgentDomainById(String id);
    /**
      * Page AgentDomain
      *
      * @param query query
      * @return page result
      */
    PageResult<AgentDomain> pageAgentDomain(PageQuery query);
}
