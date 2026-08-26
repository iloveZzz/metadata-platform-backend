package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.application.dto.BatchSummaryDTO;
import com.yss.smartdiscovery.application.dto.TagCandidateDTO;
import com.yss.smartdiscovery.domain.audit.SmartTagAuditLog;
import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.gateway.AuditLogRepository;
import com.yss.smartdiscovery.domain.gateway.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidatePoolAppService {

    private final CandidateRepository candidateRepository;
    private final AuditLogRepository auditLogRepository;

    public List<TagCandidateDTO> listCandidates(String status, String source, String domain) {
        return candidateRepository.listCandidates(status, source, domain).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BatchSummaryDTO batchApprove(List<String> candidateIds, String reason) {
        candidateRepository.updateBatchStatus(candidateIds, "MANUAL_APPROVED");
        String batchId = "BATCH-" + System.currentTimeMillis();

        auditLogRepository.save(SmartTagAuditLog.builder()
                .id(UUID.randomUUID().toString())
                .batchId(batchId)
                .actionType("MANUAL_APPROVE")
                .actionName("人工批量采纳")
                .operator("zhudaoming (Data Owner)")
                .fieldCount(candidateIds.size())
                .status("APPLIED")
                .createdAt(LocalDateTime.now())
                .build());

        return BatchSummaryDTO.builder()
                .batchId(batchId)
                .totalProcessed(candidateIds.size())
                .autoAppliedCount(0)
                .pendingReviewCount(0)
                .build();
    }

    public BatchSummaryDTO batchReject(List<String> candidateIds, String reason) {
        candidateRepository.updateBatchStatus(candidateIds, "REJECTED");
        return BatchSummaryDTO.builder()
                .batchId("BATCH-" + System.currentTimeMillis())
                .totalProcessed(candidateIds.size())
                .build();
    }

    public TagCandidateDTO modifyAndApprove(String id, String targetTag, String modifyReason) {
        SmartTagCandidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("候选建议不存在: " + id));
        candidate.setRecommendedTagName(targetTag);
        candidate.approveManually();
        candidateRepository.update(candidate);
        return toDTO(candidate);
    }

    public void rollbackBatch(String batchId) {
        SmartTagAuditLog log = auditLogRepository.findByBatchId(batchId)
                .orElseThrow(() -> new IllegalArgumentException("审计批次不存在: " + batchId));
        log.rollback();
        auditLogRepository.update(log);
    }

    public List<TagCandidateDTO> getAssetDrawerSuggestions(String tableName) {
        return candidateRepository.listCandidates(null, null, null).stream()
                .filter(c -> tableName == null || tableName.equalsIgnoreCase(c.getTableName()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private TagCandidateDTO toDTO(SmartTagCandidate c) {
        return TagCandidateDTO.builder()
                .id(c.getId())
                .tableName(c.getTableName())
                .columnName(c.getColumnName())
                .columnComment(c.getColumnComment())
                .currentTag(c.getCurrentTag())
                .recommendedTagId(c.getRecommendedTagId())
                .recommendedTagName(c.getRecommendedTagName())
                .tagCategory(c.getTagCategory())
                .source(c.getSource())
                .confidence(c.getConfidence())
                .inferenceReason(c.getInferenceReason())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
