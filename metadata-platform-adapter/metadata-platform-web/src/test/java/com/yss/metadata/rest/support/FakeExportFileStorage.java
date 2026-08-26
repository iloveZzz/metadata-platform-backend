package com.yss.metadata.rest.support;

import com.yss.metadata.domain.lineage.gateway.ExportFileStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 导出文件存储测试替身（Web 契约测试 seam；临时目录写入）。
 */
public class FakeExportFileStorage implements ExportFileStorage {

    private final Path baseDir;

    public FakeExportFileStorage() {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "metadata-export-web-test"));
    }

    public FakeExportFileStorage(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String store(String taskId, String format, String content) {
        try {
            Files.createDirectories(baseDir);
            Path file = baseDir.resolve(taskId + "." + format);
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("导出文件写入失败: " + e.getMessage(), e);
        }
    }
}
