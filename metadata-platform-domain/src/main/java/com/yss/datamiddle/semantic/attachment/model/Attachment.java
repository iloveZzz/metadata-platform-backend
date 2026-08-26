package com.yss.datamiddle.semantic.attachment.model;

import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资产挂接聚合根（AttachmentAggregate：attachment）。
 */
@Getter
@Setter
public class Attachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long assetId;
    private AttachmentLevel level;
    private String columnName;
    private SemanticObjectType semanticType;
    private Long semanticId;
    private AttachmentStatus status;
    private String releasedBy;
    private LocalDateTime releasedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Attachment create(Long assetId, AttachmentLevel level, String columnName,
                                     SemanticObjectType semanticType, Long semanticId, String operator) {
        if (assetId == null) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "assetId", "REQUIRED", "资产ID不能为空");
        }
        if (level == null) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "level", "REQUIRED", "挂接层级不能为空");
        }
        if (level == AttachmentLevel.COLUMN && (columnName == null || columnName.trim().isEmpty())) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "columnName", "REQUIRED", "列级挂接必须指定列名");
        }
        if (level == AttachmentLevel.TABLE && columnName != null && !columnName.trim().isEmpty()) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "columnName", "FORBIDDEN", "表级挂接不可指定列名");
        }

        Attachment a = new Attachment();
        a.assetId = assetId;
        a.level = level;
        a.columnName = level == AttachmentLevel.COLUMN ? columnName.trim() : null;
        a.semanticType = semanticType;
        a.semanticId = semanticId;
        a.status = AttachmentStatus.ACTIVE;
        a.createdBy = operator;
        a.createdAt = LocalDateTime.now();
        a.updatedAt = a.createdAt;
        return a;
    }

    public void release(String operator) {
        if (this.status == AttachmentStatus.RELEASED) {
            return; // 幂等
        }
        this.status = AttachmentStatus.RELEASED;
        this.releasedBy = operator;
        this.releasedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
