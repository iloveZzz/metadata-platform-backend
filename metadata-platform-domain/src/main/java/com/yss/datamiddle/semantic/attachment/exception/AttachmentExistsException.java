package com.yss.datamiddle.semantic.attachment.exception;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;

public class AttachmentExistsException extends StateConflictException {
    private final Long existingAttachmentId;

    public AttachmentExistsException(Long existingAttachmentId) {
        super("ATTACHMENT_EXISTS: 资产字段已挂接该语义对象，既有挂接 ID: " + existingAttachmentId);
        this.existingAttachmentId = existingAttachmentId;
    }

    public Long getExistingAttachmentId() {
        return existingAttachmentId;
    }
}
