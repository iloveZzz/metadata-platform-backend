package com.yss.metadata.infrastructure.export;

import com.yss.metadata.domain.lineage.gateway.ExportFileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 导出文件本地存储测试（WU-03-04；本地可配置目录 seam）。
 */
class LocalExportFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("store 写入 {taskId}.{format} 文件并返回绝对路径引用")
    void storeWritesFile() throws Exception {
        ExportFileStorage storage = new LocalExportFileStorage(tempDir.toString());

        String fileRef = storage.store("task-1", "csv", "a,b\n1,2\n");

        Path file = Paths.get(fileRef);
        assertThat(Files.exists(file)).isTrue();
        assertThat(file.getFileName().toString()).isEqualTo("task-1.csv");
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertThat(content).isEqualTo("a,b\n1,2\n");
    }

    @Test
    @DisplayName("目录自动创建")
    void createsDirectory() {
        ExportFileStorage storage = new LocalExportFileStorage(
                tempDir.resolve("sub").resolve("exports").toString());

        String fileRef = storage.store("task-2", "json", "{}");

        assertThat(Files.exists(Paths.get(fileRef))).isTrue();
    }

    @Test
    @DisplayName("不可写目录抛出存储异常（任务标记 failed 的依据）")
    void storeFailureThrows() {
        Path readOnlyDir = tempDir.resolve("no-write");
        // 占位为文件使其无法建目录
        assertThatThrownBy(() -> {
            Files.createFile(readOnlyDir);
            new LocalExportFileStorage(readOnlyDir.toString()).store("task-3", "csv", "x");
        }).isInstanceOf(IllegalStateException.class);
    }
}
