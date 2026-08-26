package com.yss.datamiddle.semantic.attachment.gateway;

import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;

import java.util.List;
import java.util.Optional;

public interface AttachmentGateway {
    Attachment save(Attachment attachment);
    Attachment update(Attachment attachment);
    Optional<Attachment> findById(Long id);
    Optional<Attachment> findActiveAttachment(Long assetId, String columnName, SemanticObjectType semanticType, Long semanticId);
    List<Attachment> query(Long assetId, String columnName, SemanticObjectType semanticType, Long semanticId, AttachmentStatus status);
}
