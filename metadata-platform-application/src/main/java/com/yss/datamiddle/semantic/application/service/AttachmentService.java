package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.AttachmentCreateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.attachment.exception.AttachmentExistsException;
import com.yss.datamiddle.semantic.attachment.gateway.AttachmentGateway;
import com.yss.datamiddle.semantic.attachment.gateway.SemanticAssetGateway;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 资产挂接应用服务（SL-004 / SL-007 / SL-009）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentGateway attachmentGateway;
    private final SemanticAssetGateway semanticAssetGateway;
    private final CurrentUserPort currentUserPort;

    public Attachment create(AttachmentCreateInput input) {
        checkWritePermission();
        String operator = currentUserPort.userName();

        // 校验是否已存在活跃挂接 (SB-03)
        Optional<Attachment> existing = attachmentGateway.findActiveAttachment(
                input.getAssetId(),
                input.getColumnName(),
                input.getSemanticType(),
                input.getSemanticId()
        );
        if (existing.isPresent()) {
            throw new AttachmentExistsException(existing.get().getId());
        }

        Attachment attachment = Attachment.create(
                input.getAssetId(),
                input.getLevel(),
                input.getColumnName(),
                input.getSemanticType(),
                input.getSemanticId(),
                operator
        );
        return attachmentGateway.save(attachment);
    }

    public void release(Long id) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        Attachment attachment = attachmentGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂接不存在: " + id));
        attachment.release(operator);
        attachmentGateway.update(attachment);
    }

    public List<Attachment> query(Long assetId, String columnName, SemanticObjectType semanticType, Long semanticId, AttachmentStatus status) {
        return attachmentGateway.query(assetId, columnName, semanticType, semanticId, status);
    }

    public Attachment getById(Long id) {
        return attachmentGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("挂接不存在: " + id));
    }

    private void checkWritePermission() {
        if (!currentUserPort.isWritePermitted()) {
            throw new PermissionDeniedException("只读用户禁止执行写操作");
        }
    }
}
