package com.yss.datamiddle.smartgovernance.infrastructure.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datamiddle.smartgovernance.domain.metric.gateway.MetricConflictGateway;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictStatus;
import com.yss.datamiddle.smartgovernance.domain.metric.model.ConflictType;
import com.yss.datamiddle.smartgovernance.domain.metric.model.MetricConflictRecord;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper.MetricConflictMapper;
import com.yss.datamiddle.smartgovernance.infrastructure.repository.po.MetricConflictPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MetricConflictGatewayImpl implements MetricConflictGateway {

    private final MetricConflictMapper conflictMapper;

    @Override
    public void save(MetricConflictRecord record) {
        conflictMapper.insert(toPO(record));
    }

    @Override
    public void batchSave(List<MetricConflictRecord> records) {
        if (records != null && !records.isEmpty()) {
            for (MetricConflictRecord r : records) {
                conflictMapper.insert(toPO(r));
            }
        }
    }

    @Override
    public Optional<MetricConflictRecord> findById(String id) {
        return Optional.ofNullable(conflictMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public void update(MetricConflictRecord record) {
        conflictMapper.updateById(toPO(record));
    }

    @Override
    public List<MetricConflictRecord> queryConflicts(
            Integer pageIndex,
            Integer pageSize,
            ConflictStatus status,
            ConflictType conflictType,
            String keyword
    ) {
        LambdaQueryWrapper<MetricConflictPO> wrapper = buildWrapper(status, conflictType, keyword);
        wrapper.orderByDesc(MetricConflictPO::getCreatedAt);

        int current = (pageIndex != null && pageIndex > 0) ? pageIndex : 1;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 20;

        Page<MetricConflictPO> page = new Page<>(current, size);
        return conflictMapper.selectPage(page, wrapper).getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countConflicts(ConflictStatus status, ConflictType conflictType, String keyword) {
        return conflictMapper.selectCount(buildWrapper(status, conflictType, keyword));
    }

    private LambdaQueryWrapper<MetricConflictPO> buildWrapper(ConflictStatus status, ConflictType conflictType, String keyword) {
        LambdaQueryWrapper<MetricConflictPO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(MetricConflictPO::getStatus, status.name());
        }
        if (conflictType != null) {
            wrapper.eq(MetricConflictPO::getConflictType, conflictType.name());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(MetricConflictPO::getIndicatorAName, k)
                    .or().like(MetricConflictPO::getIndicatorBName, k)
                    .or().like(MetricConflictPO::getConflictCode, k));
        }
        return wrapper;
    }

    private MetricConflictRecord toDomain(MetricConflictPO po) {
        if (po == null) return null;
        return MetricConflictRecord.builder()
                .id(po.getId())
                .conflictCode(po.getConflictCode())
                .indicatorAId(po.getIndicatorAId())
                .indicatorAName(po.getIndicatorAName())
                .indicatorACode(po.getIndicatorACode())
                .indicatorADomain(po.getIndicatorADomain())
                .indicatorBId(po.getIndicatorBId())
                .indicatorBName(po.getIndicatorBName())
                .indicatorBCode(po.getIndicatorBCode())
                .indicatorBDomain(po.getIndicatorBDomain())
                .conflictType(po.getConflictType() != null ? ConflictType.valueOf(po.getConflictType()) : ConflictType.FORMULA_DRIFT)
                .similarityScore(po.getSimilarityScore())
                .formulaA(po.getFormulaA())
                .formulaB(po.getFormulaB())
                .astDiffSummary(po.getAstDiffSummary())
                .status(po.getStatus() != null ? ConflictStatus.valueOf(po.getStatus()) : ConflictStatus.UNRESOLVED)
                .canonicalId(po.getCanonicalId())
                .resolutionComment(po.getResolutionComment())
                .operator(po.getOperator())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private MetricConflictPO toPO(MetricConflictRecord d) {
        if (d == null) return null;
        return MetricConflictPO.builder()
                .id(d.getId())
                .conflictCode(d.getConflictCode())
                .indicatorAId(d.getIndicatorAId())
                .indicatorAName(d.getIndicatorAName())
                .indicatorACode(d.getIndicatorACode())
                .indicatorADomain(d.getIndicatorADomain())
                .indicatorBId(d.getIndicatorBId())
                .indicatorBName(d.getIndicatorBName())
                .indicatorBCode(d.getIndicatorBCode())
                .indicatorBDomain(d.getIndicatorBDomain())
                .conflictType(d.getConflictType() != null ? d.getConflictType().name() : ConflictType.FORMULA_DRIFT.name())
                .similarityScore(d.getSimilarityScore())
                .formulaA(d.getFormulaA())
                .formulaB(d.getFormulaB())
                .astDiffSummary(d.getAstDiffSummary())
                .status(d.getStatus() != null ? d.getStatus().name() : ConflictStatus.UNRESOLVED.name())
                .canonicalId(d.getCanonicalId())
                .resolutionComment(d.getResolutionComment())
                .operator(d.getOperator())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
