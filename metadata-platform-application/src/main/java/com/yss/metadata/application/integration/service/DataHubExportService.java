package com.yss.metadata.application.integration.service;

import com.yss.metadata.client.vo.ExportTaskVO;

/**
 * DataHub 导出应用服务（FR-021；WU-05-04）。
 *
 * <p>POST /api/exports/datahub：202 异步任务幂等（复用 export_task：
 * asset_id=NULL 全局导出 + format=datahub）+ DataHubExporter SPI + 审计
 * （integration.datahub-export）。目标未配置抛非法参数（422）。</p>
 */
public interface DataHubExportService {

    /**
     * 触发 DataHub 导出（202 ExportTaskVO；同 asset_id+format 进行中任务幂等复用）。
     */
    ExportTaskVO trigger(String operator);
}
