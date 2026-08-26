package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * DataHub 导出结果值对象（DataHubExporter SPI 输出）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHubExportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否导出成功 */
    private boolean success;

    /** 结果 / 失败文案（写入 export_task.file_ref 或失败原因） */
    private String message;

    public static DataHubExportResult success(String message) {
        return DataHubExportResult.builder().success(true).message(message).build();
    }

    public static DataHubExportResult failure(String message) {
        return DataHubExportResult.builder().success(false).message(message).build();
    }
}
