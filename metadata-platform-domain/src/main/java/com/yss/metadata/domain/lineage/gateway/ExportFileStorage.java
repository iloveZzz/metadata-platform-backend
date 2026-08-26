package com.yss.metadata.domain.lineage.gateway;

/**
 * 导出文件存储端口（可替换 seam）。
 *
 * <p>本切片实现为本地可配置目录（LocalExportFileStorage）；对象存储
 * （OSS/S3 等）后续替换（seam_deferred 登记）。</p>
 */
public interface ExportFileStorage {

    /**
     * 存储导出文件内容并返回文件引用（file_ref）。
     *
     * @param taskId  导出任务 id（文件名一部分）
     * @param format  csv/json
     * @param content 文件内容
     */
    String store(String taskId, String format, String content);
}
