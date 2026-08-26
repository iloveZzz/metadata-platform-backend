package com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AuditLog;

import java.util.Optional;

/**
 * 审计留痕端口（调用即写，不可变 SEC-06，数据架构 §5 / §6.1）。
 *
 * <p>不可变性：仅暴露 append-only 写入与只读查询；<b>不提供 update / delete 路径</b>
 * （SEC-06「数据库账号最小权限，无修改 / 删除路径」的端口级表达，WU-01-02 生成骨架
 * 中的 updateAuditLog / deleteAuditLog 已移除，不可接线——证据记录于
 * verification/slice-01/execution-result.md）。</p>
 */
public interface AuditLogGateway{
    /**
     * Add AuditLog
     *
     * @param entity entity
     * @return id
     */
    String addAuditLog(AuditLog entity);
    /**
     * Get AuditLog by id
     *
     * @param id id
     * @return optional
     */
    Optional<AuditLog> getAuditLogById(String id);
    /**
      * Page AuditLog
      *
      * @param query query
      * @return page result
      */
    PageResult<AuditLog> pageAuditLog(PageQuery query);
}
