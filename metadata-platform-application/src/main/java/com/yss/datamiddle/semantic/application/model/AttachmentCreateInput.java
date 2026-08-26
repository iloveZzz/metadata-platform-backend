package com.yss.datamiddle.semantic.application.model;

import com.yss.datamiddle.semantic.attachment.model.AttachmentLevel;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentCreateInput {
    private Long assetId;
    private AttachmentLevel level;
    private String columnName;
    private SemanticObjectType semanticType;
    private Long semanticId;
}
