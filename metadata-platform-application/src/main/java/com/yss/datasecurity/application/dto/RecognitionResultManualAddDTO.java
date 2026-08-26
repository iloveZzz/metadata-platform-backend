package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultManualAddDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    /**
     * 去重处理策略:
     * OVERWRITE_ALL (覆盖已有识别结果)
     * OVERWRITE_UNLOCKED (仅覆盖已有自动识别结果/未锁定)
     * RETAIN_EXISTING (保留已有识别结果不更新)
     */
    @NotBlank(message = "请选择去重策略")
    private String dedupStrategy;

    @Valid
    @NotEmpty(message = "已添加记录列表不能为空")
    private List<ManualAddRecordItemDTO> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualAddRecordItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String datasourceId;
        private String datasourceName;
        private String schemaName;
        @NotBlank(message = "数据表名不能为空")
        private String tableName;
        @NotBlank(message = "字段名不能为空")
        private String fieldName;
        private String fieldComment;
        private String assetSourceType; // DATAPHIN / DATASOURCE
        private String assetSourceInfo;
        @NotNull(message = "数据分类不能为空")
        private Long categoryId;
        private Long securityGradeId;
        private String maskingStatus; // ENABLED / DISABLED
    }
}
