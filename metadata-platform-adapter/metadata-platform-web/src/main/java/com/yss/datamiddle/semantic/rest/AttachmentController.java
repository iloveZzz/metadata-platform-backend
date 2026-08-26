package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.semantic.application.model.AttachmentCreateInput;
import com.yss.datamiddle.semantic.application.service.AttachmentService;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentLevel;
import com.yss.datamiddle.semantic.attachment.model.AttachmentStatus;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
import com.yss.datamiddle.semantic.client.dto.cmd.AttachmentCreateCmd;
import com.yss.datamiddle.semantic.client.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产挂接 REST API 控制器（SL-004 / SL-007 / SL-009）。
 */
@RestController
@RequestMapping("/api/semantic/attachments")
@RequiredArgsConstructor
@Validated
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<AttachmentVO> create(@Valid @RequestBody AttachmentCreateCmd cmd) {
        AttachmentCreateInput input = AttachmentCreateInput.builder()
                .assetId(cmd.getAssetId())
                .level(AttachmentLevel.valueOf(cmd.getLevel().toUpperCase()))
                .columnName(cmd.getColumnName())
                .semanticType(SemanticObjectType.valueOf(cmd.getSemanticType().toUpperCase()))
                .semanticId(cmd.getSemanticId())
                .build();
        Attachment created = attachmentService.create(input);
        return SingleResult.of(toVO(created));
    }

    @GetMapping
    public MultiResult<AttachmentVO> query(
            @RequestParam(value = "assetId", required = false) Long assetId,
            @RequestParam(value = "columnName", required = false) String columnName,
            @RequestParam(value = "semanticType", required = false) String semanticType,
            @RequestParam(value = "semanticId", required = false) Long semanticId,
            @RequestParam(value = "status", required = false) String status
    ) {
        SemanticObjectType type = semanticType != null ? SemanticObjectType.valueOf(semanticType.toUpperCase()) : null;
        AttachmentStatus attStatus = status != null ? AttachmentStatus.valueOf(status.toUpperCase()) : null;

        List<AttachmentVO> list = attachmentService.query(assetId, columnName, type, semanticId, attStatus).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return MultiResult.of(list);
    }

    @GetMapping("/{id}")
    public SingleResult<AttachmentVO> getById(@PathVariable("id") Long id) {
        Attachment a = attachmentService.getById(id);
        return SingleResult.of(toVO(a));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        attachmentService.release(id);
    }

    private AttachmentVO toVO(Attachment a) {
        return AttachmentVO.builder()
                .id(a.getId())
                .assetId(a.getAssetId())
                .level(a.getLevel() != null ? a.getLevel().name() : null)
                .columnName(a.getColumnName())
                .semanticType(a.getSemanticType() != null ? a.getSemanticType().name() : null)
                .semanticId(a.getSemanticId())
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .releasedBy(a.getReleasedBy())
                .releasedAt(a.getReleasedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
