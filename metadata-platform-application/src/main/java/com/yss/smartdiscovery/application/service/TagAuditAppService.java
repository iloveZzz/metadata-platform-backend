package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.application.dto.TagAuditLogDTO;
import com.yss.smartdiscovery.domain.audit.SmartTagAuditLog;
import com.yss.smartdiscovery.domain.gateway.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagAuditAppService {

    private final AuditLogRepository auditLogRepository;

    public List<TagAuditLogDTO> listAuditLogs() {
        return auditLogRepository.listLogs().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private TagAuditLogDTO toDTO(SmartTagAuditLog log) {
        return TagAuditLogDTO.builder()
                .id(log.getId())
                .batchId(log.getBatchId())
                .actionType(log.getActionType())
                .actionName(log.getActionName())
                .operator(log.getOperator())
                .fieldCount(log.getFieldCount())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
