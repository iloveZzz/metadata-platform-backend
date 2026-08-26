package com.yss.metadata.application.integration.service;

import com.yss.metadata.client.dto.cmd.IntegrationConfigCmd;
import com.yss.metadata.client.vo.IntegrationVO;

/**
 * 集成配置应用服务（FR-026；WU-05-01）。
 *
 * <p>GET /api/integrations 组合配置（Gravitino/DataHub/OpenLineage 统计）；
 * PUT 保存（test=true 先测试 Gravitino 连接，失败 422 不保存；test=false 仅保存）。</p>
 */
public interface IntegrationService {

    /**
     * 查询集成配置（组合 VO；0 配置空结构非错误）。
     */
    IntegrationVO getConfig();

    /**
     * 保存集成配置（幂等 upsert 单例行 + 审计；test=true 时测试连接失败抛 422）。
     */
    IntegrationVO saveConfig(IntegrationConfigCmd cmd, String operator);
}
