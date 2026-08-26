package com.yss.metadata.application.lineage.support;

import com.yss.metadata.domain.lineage.gateway.ExportFileStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 导出文件存储测试替身（写入临时目录；支持注入失败验证 failed 状态流转）。
 */
public class FakeExportFileStorage implements ExportFileStorage {

    private final Path baseDir;

    private boolean failNext;

    public FakeExportFileStorage() {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "metadata-export-test"));
    }

    public FakeExportFileStorage(Path baseDir) {
        this.baseDir = baseDir;
    }

    public void failNext(boolean fail) {
        this.failNext = fail;
    }

    @Override
    public String store(String taskId, String format, String content) {
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("测试注入的存储失败");
        }
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
