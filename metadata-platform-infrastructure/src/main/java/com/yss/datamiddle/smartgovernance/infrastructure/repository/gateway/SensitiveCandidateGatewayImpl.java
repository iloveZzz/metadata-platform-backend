package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datamiddle.smartgovernance.domain.security.gateway.SensitiveCandidateGateway;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.FunnelLayer;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.SensitiveCandidateMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.SensitiveCandidatePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SensitiveCandidateGatewayImpl implements SensitiveCandidateGateway {

    private final SensitiveCandidateMapper candidateMapper;

    @Override
    public void batchSave(List<SensitiveCandidate> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            for (SensitiveCandidate c : candidates) {
                candidateMapper.insert(toPO(c));
            }
        }
    }

    @Override
    public Optional<SensitiveCandidate> findById(String id) {
        return Optional.ofNullable(candidateMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SensitiveCandidate> findByIds(List<String> ids) {
        return candidateMapper.selectBatchIds(ids).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void batchUpdate(List<SensitiveCandidate> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            for (SensitiveCandidate c : candidates) {
                candidateMapper.updateById(toPO(c));
            }
        }
    }

    @Override
    public List<SensitiveCandidate> queryCandidates(
            Integer pageIndex,
            Integer pageSize,
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    ) {
        LambdaQueryWrapper<SensitiveCandidatePO> wrapper = buildQueryWrapper(status, securityLevel, sensitiveType, keyword);
        wrapper.orderByDesc(SensitiveCandidatePO::getCreatedAt);

        int current = (pageIndex != null && pageIndex > 0) ? pageIndex : 1;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 20;

        Page<SensitiveCandidatePO> page = new Page<>(current, size);
        return candidateMapper.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countCandidates(CandidateStatus status, SecurityLevel securityLevel, String sensitiveType, String keyword) {
        LambdaQueryWrapper<SensitiveCandidatePO> wrapper = buildQueryWrapper(status, securityLevel, sensitiveType, keyword);
        return candidateMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<SensitiveCandidatePO> buildQueryWrapper(
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    ) {
        LambdaQueryWrapper<SensitiveCandidatePO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SensitiveCandidatePO::getStatus, status.getCode());
        }
        if (securityLevel != null) {
            wrapper.eq(SensitiveCandidatePO::getRecommendedLevel, securityLevel.getCode());
        }
        if (sensitiveType != null && !sensitiveType.trim().isEmpty()) {
            wrapper.eq(SensitiveCandidatePO::getSensitiveType, sensitiveType.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(SensitiveCandidatePO::getColumnName, k)
                    .or().like(SensitiveCandidatePO::getTableName, k)
                    .or().like(SensitiveCandidatePO::getReasoning, k));
        }
        return wrapper;
    }

    private SensitiveCandidate toDomain(SensitiveCandidatePO po) {
        if (po == null) return null;
        return SensitiveCandidate.builder()
                .id(po.getId())
                .templateId(po.getTemplateId())
                .ruleId(po.getRuleId())
                .dataSource(po.getDataSource())
                .databaseName(po.getDatabaseName())
                .tableName(po.getTableName())
                .columnName(po.getColumnName())
                .columnComment(po.getColumnComment())
                .dataType(po.getDataType())
                .sensitiveType(po.getSensitiveType())
                .recommendedLevel(SecurityLevel.of(po.getRecommendedLevel()))
                .clauseRef(po.getClauseRef())
                .reasoning(po.getReasoning())
                .confidence(po.getConfidence())
                .funnelLayer(po.getFunnelLayer() != null ? FunnelLayer.valueOf(po.getFunnelLayer()) : FunnelLayer.L1_REGEX)
                .status(po.getStatus() != null ? CandidateStatus.valueOf(po.getStatus()) : CandidateStatus.PENDING)
                .actualLevel(po.getActualLevel() != null ? SecurityLevel.of(po.getActualLevel()) : null)
                .operator(po.getOperator())
                .reviewComment(po.getReviewComment())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private SensitiveCandidatePO toPO(SensitiveCandidate d) {
        if (d == null) return null;
        return SensitiveCandidatePO.builder()
                .id(d.getId())
                .templateId(d.getTemplateId())
                .ruleId(d.getRuleId())
                .dataSource(d.getDataSource())
                .databaseName(d.getDatabaseName())
                .tableName(d.getTableName())
                .columnName(d.getColumnName())
                .columnComment(d.getColumnComment())
                .dataType(d.getDataType())
                .sensitiveType(d.getSensitiveType())
                .recommendedLevel(d.getRecommendedLevel() != null ? d.getRecommendedLevel().getCode() : "L1")
                .clauseRef(d.getClauseRef())
                .reasoning(d.getReasoning())
                .confidence(d.getConfidence())
                .funnelLayer(d.getFunnelLayer() != null ? d.getFunnelLayer().name() : FunnelLayer.L1_REGEX.name())
                .status(d.getStatus() != null ? d.getStatus().name() : CandidateStatus.PENDING.name())
                .actualLevel(d.getActualLevel() != null ? d.getActualLevel().getCode() : null)
                .operator(d.getOperator())
                .reviewComment(d.getReviewComment())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
