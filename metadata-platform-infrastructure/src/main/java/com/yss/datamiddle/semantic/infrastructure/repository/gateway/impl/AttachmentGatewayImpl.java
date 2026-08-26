package com.yss.datamiddle.semantic.infrastructure.repository.gateway.impl;

import com.yss.datamiddle.semantic.attachment.gateway.AttachmentGateway;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 资产挂接持久化网关实现（SL-004）。
 */
@Repository
public class AttachmentGatewayImpl implements AttachmentGateway {

    private final Map<Long, Attachment> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(3000);

    @Override
    public Attachment save(Attachment attachment) {
        if (attachment.getId() == null) {
            attachment.setId(idGen.incrementAndGet());
        }
        storage.put(attachment.getId(), attachment);
        return attachment;
    }

    @Override
    public Attachment update(Attachment attachment) {
        storage.put(attachment.getId(), attachment);
        return attachment;
    }

    @Override
    public Optional<Attachment> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Attachment> findActiveAttachment(Long assetId, String columnName, SemanticObjectType semanticType, Long semanticId) {
        return storage.values().stream()
                .filter(a -> a.getStatus() == AttachmentStatus.ACTIVE
                        && Objects.equals(a.getAssetId(), assetId)
                        && Objects.equals(a.getColumnName(), columnName)
                        && a.getSemanticType() == semanticType
                        && Objects.equals(a.getSemanticId(), semanticId))
                .findFirst();
    }

    @Override
    public List<Attachment> query(Long assetId, String columnName, SemanticObjectType semanticType, Long semanticId, AttachmentStatus status) {
        return storage.values().stream()
                .filter(a -> assetId == null || Objects.equals(a.getAssetId(), assetId))
                .filter(a -> columnName == null || Objects.equals(a.getColumnName(), columnName))
                .filter(a -> semanticType == null || a.getSemanticType() == semanticType)
                .filter(a -> semanticId == null || Objects.equals(a.getSemanticId(), semanticId))
                .filter(a -> status == null || a.getStatus() == status)
                .collect(Collectors.toList());
    }
}
