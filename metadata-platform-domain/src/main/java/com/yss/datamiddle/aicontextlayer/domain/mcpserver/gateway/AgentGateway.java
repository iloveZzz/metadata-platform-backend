package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.Agent;

import java.util.Optional;

public interface AgentGateway{
    /**
     * Add Agent
     *
     * @param entity entity
     * @return id
     */
    String addAgent(Agent entity);
    /**
     * Update Agent
     *
     * @param entity entity
     * @return boolean
     */
    boolean updateAgent(Agent entity);
    /**
     * Delete Agent
     *
     * @param id id
     * @return boolean
     */
    boolean deleteAgent(String id);
    /**
     * Get Agent by id
     *
     * @param id id
     * @return optional
     */
    Optional<Agent> getAgentById(String id);
    /**
      * Page Agent
      *
      * @param query query
      * @return page result
      */
    PageResult<Agent> pageAgent(PageQuery query);
}
