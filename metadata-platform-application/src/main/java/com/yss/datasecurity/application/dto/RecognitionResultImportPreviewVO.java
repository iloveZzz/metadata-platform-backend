package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultImportPreviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalCount;
    private Integer validCount;
    private Integer errorCount;
    private Integer duplicateCount;

    private List<ImportRowPreviewItemVO> validRows;
    private List<ImportRowPreviewItemVO> errorRows;
    private List<ImportRowPreviewItemVO> duplicateRows;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowPreviewItemVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer rowNumber;
        private String tableName;
        private String fieldName;
        private String categoryName;
        private String securityGradeName;
        private String onlineCategoryName;
        private String status; // VALID / ERROR / DUPLICATE
        private String errorMessage;
    }
}
