package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.AttachmentCreateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.attachment.exception.AttachmentExistsException;
import com.yss.datamiddle.semantic.attachment.gateway.AttachmentGateway;
import com.yss.datamiddle.semantic.attachment.gateway.SemanticAssetGateway;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentLevel;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AttachmentServiceTest {

    private final AttachmentGateway attachmentGateway = Mockito.mock(AttachmentGateway.class);
    private final SemanticAssetGateway assetGateway = Mockito.mock(SemanticAssetGateway.class);
    private final CurrentUserPort userPort = new CurrentUserPort() {
        @Override
        public String userName() {
            return "user1";
        }

        @Override
        public boolean isWritePermitted() {
            return true;
        }
    };

    private final AttachmentService service = new AttachmentService(attachmentGateway, assetGateway, userPort);

    @Test
    @DisplayName("SB-03: 重复挂接抛出 409 ATTACHMENT_EXISTS")
    void duplicateAttachmentThrows409() {
        Attachment existing = new Attachment();
        existing.setId(99L);
        when(attachmentGateway.findActiveAttachment(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        AttachmentCreateInput input = AttachmentCreateInput.builder()
                .assetId(10L)
                .level(AttachmentLevel.TABLE)
                .semanticType(SemanticObjectType.TERM)
                .semanticId(1L)
                .build();

        assertThrows(AttachmentExistsException.class, () -> service.create(input));
    }

    @Test
    @DisplayName("正常创建挂接")
    void createSuccess() {
        when(attachmentGateway.findActiveAttachment(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(attachmentGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AttachmentCreateInput input = AttachmentCreateInput.builder()
                .assetId(10L)
                .level(AttachmentLevel.TABLE)
                .semanticType(SemanticObjectType.TERM)
                .semanticId(1L)
                .build();

        Attachment created = service.create(input);
        assertNotNull(created);
    }
}
