package com.yss.metadata.infrastructure.export;

import com.yss.metadata.domain.lineage.gateway.ExportFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 导出文件本地存储实现（可替换 seam；本地可配置目录）。
 *
 * <p>目录由配置 {@code metadata.export.dir} 指定（默认 ./data/exports）；
 * 文件命名 {taskId}.{csv|json}，返回绝对路径作为 file_ref。
 * 对象存储（OSS/S3 等）后续替换（seam_deferred 登记）。</p>
 */
@Slf4j
@Component
public class LocalExportFileStorage implements ExportFileStorage {

    private final Path baseDir;

    public LocalExportFileStorage(@Value("${metadata.export.dir:./data/exports}") String exportDir) {
        this.baseDir = Paths.get(exportDir);
    }

    @Override
    public String store(String taskId, String format, String content) {
        try {
            Files.createDirectories(baseDir);
            Path file = baseDir.resolve(taskId + "." + format);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            log.info("导出文件已写入，taskId={}, format={}, file={}", taskId, format, file.toAbsolutePath());
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("导出文件写入失败: " + e.getMessage(), e);
        }
    }
}
