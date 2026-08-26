package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ToolRegistry;

import java.util.Optional;

public interface ToolRegistryGateway{
    /**
     * Add ToolRegistry
     *
     * @param entity entity
     * @return id
     */
    String addToolRegistry(ToolRegistry entity);
    /**
     * Update ToolRegistry
     *
     * @param entity entity
     * @return boolean
     */
    boolean updateToolRegistry(ToolRegistry entity);
    /**
     * Delete ToolRegistry
     *
     * @param toolName id
     * @return boolean
     */
    boolean deleteToolRegistry(String toolName);
    /**
     * Get ToolRegistry by id
     *
     * @param toolName id
     * @return optional
     */
    Optional<ToolRegistry> getToolRegistryById(String toolName);
    /**
      * Page ToolRegistry
      *
      * @param query query
      * @return page result
      */
    PageResult<ToolRegistry> pageToolRegistry(PageQuery query);
}
