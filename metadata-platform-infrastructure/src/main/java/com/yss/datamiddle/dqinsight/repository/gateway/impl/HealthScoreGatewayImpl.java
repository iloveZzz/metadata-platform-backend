package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.datamiddle.dqinsight.client.dto.query.HealthScorePageQuery;
import com.yss.datamiddle.dqinsight.client.vo.AlgorithmVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.AssetHealthRowVO;
import com.yss.datamiddle.dqinsight.client.vo.FieldHealthVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleDetailVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleScoreVO;
import com.yss.datamiddle.dqinsight.client.vo.RuleWeightVO;
import com.yss.datamiddle.dqinsight.domain.gateway.HealthScoreGateway;
import com.yss.datamiddle.dqinsight.domain.model.BandFilter;
import com.yss.datamiddle.dqinsight.domain.model.HealthScore;
import com.yss.datamiddle.dqinsight.domain.model.RuleScoreSnapshot;
import com.yss.datamiddle.dqinsight.domain.model.RuleWeight;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.domain.service.HealthScoreEngine;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.DqHealthScoreRepository;
import com.yss.datamiddle.dqinsight.repository.DqRuleDetailRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqHealthScoreConvertor;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqRuleDetailConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqHealthScorePO;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleDetailPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康分仓储实现（dq_health_score upsert + dq_rule_detail 快照 + 查询投影，数据架构 §5/§7）。
 *
 * <p>写：按 (asset_id, field_name) 唯一键 upsert（资产级 field_name NULL 与字段级共用一表，
 * 每资产 / 字段保留最新，计算幂等可重算）；规则明细按 (batch_id, asset_id, field_name, rule_name) upsert。
 * 读：资产级列表（field_name IS NULL）档位 / 独立展示态筛选分页；详情 / 钻取按最新计算批次对齐；
 * 过期展示态由查询派生（validUntil &lt; now → expired，C23），与「无结果」独立展示态不混淆。</p>
 */
@Repository
@RequiredArgsConstructor
public class HealthScoreGatewayImpl implements HealthScoreGateway {

    /** 分数来源区公式（透明可解释，与计算逻辑一致，DQI-004） */
    private static final String SCORE_FORMULA =
            "健康分 = Σ(规则权重 × 规则得分)，规则得分 passed=100 / warn=80 / failed|error=0";

    private final DqHealthScoreRepository dqHealthScoreRepository;
    private final DqRuleDetailRepository dqRuleDetailRepository;
    private final DqBatchRepository dqBatchRepository;
    private final DqHealthScoreConvertor dqHealthScoreConvertor;
    private final DqRuleDetailConvertor dqRuleDetailConvertor;
    private final HealthScoreEngine healthScoreEngine = new HealthScoreEngine();

    @Override
    public HealthScore upsert(HealthScore score) {
        DqHealthScorePO po = dqHealthScoreConvertor.toPO(score);
        DqHealthScorePO existing = findByAssetAndField(score.getAssetId(), score.getFieldName());
        if (existing != null) {
            po.setId(existing.getId());
            dqHealthScoreRepository.updateById(po);
            score.assignId(existing.getId());
        } else {
            dqHealthScoreRepository.insert(po);
            score.assignId(po.getId());
        }
        return score;
    }

    @Override
    public void saveRuleDetails(Long batchId, String assetId, String fieldName,
            List<RuleScoreSnapshot> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<String, DqRuleDetailPO> existingByRule = existingDetails(batchId, assetId, fieldName);
        for (RuleScoreSnapshot detail : details) {
            DqRuleDetailPO po = dqRuleDetailConvertor.toPO(detail);
            DqRuleDetailPO existing = existingByRule.get(detail.getRuleName());
            if (existing != null) {
                po.setId(existing.getId());
                dqRuleDetailRepository.updateById(po);
            } else {
                dqRuleDetailRepository.insert(po);
            }
        }
    }

    @Override
    public String findLatestRuleVersion(String assetId, String fieldName) {
        DqHealthScorePO po = findByAssetAndField(assetId, fieldName);
        return po == null ? null : po.getRuleVersion();
    }

    @Override
    public List<AssetHealthRowVO> listAssetHealth(HealthScorePageQuery query) {
        LambdaQueryWrapper<DqHealthScorePO> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(DqHealthScorePO::getFieldName); // 资产级行
        if (query.getAssetId() != null && !query.getAssetId().trim().isEmpty()) {
            wrapper.eq(DqHealthScorePO::getAssetId, query.getAssetId().trim());
        }
        if (query.getDomain() != null && !query.getDomain().trim().isEmpty()) {
            wrapper.eq(DqHealthScorePO::getDomain, query.getDomain().trim());
        }
        if (query.getAssetType() != null && !query.getAssetType().trim().isEmpty()) {
            wrapper.eq(DqHealthScorePO::getAssetType, query.getAssetType().trim());
        }
        // 数据域过滤 seam（DataDomainFilter → query.visibleDomains；null / 空 = 不限制，C24）
        if (query.getVisibleDomains() != null && !query.getVisibleDomains().isEmpty()) {
            wrapper.in(DqHealthScorePO::getDomain, query.getVisibleDomains());
        }
        applyBandFilter(wrapper, query.getBand());
        applySort(wrapper, query.getSort());

        com.baomidou.mybatisplus.core.metadata.IPage<DqHealthScorePO> page = dqHealthScoreRepository.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageIndex(), query.getPageSize()), wrapper);
        query.setTempTotalCount(page.getTotal());
        List<DqHealthScorePO> pos = page.getRecords();
        Instant now = Instant.now();
        return pos.stream()
                .map(po -> dqHealthScoreConvertor.toAssetHealthRowVO(po, now))
                .collect(Collectors.toList());
    }

    /**
     * 资产列表排序（仪表盘 sort：score 降序 / lastResultAt 降序 / name 升序；
     * 缺省 = 最近结果时间倒序，与 health 列表既有默认一致，冻结契约 m3）。
     */
    private void applySort(LambdaQueryWrapper<DqHealthScorePO> wrapper, com.yss.datamiddle.dqinsight.domain.model.DashboardSort sort) {
        if (sort == com.yss.datamiddle.dqinsight.domain.model.DashboardSort.SCORE) {
            wrapper.orderByDesc(DqHealthScorePO::getScore);
        } else if (sort == com.yss.datamiddle.dqinsight.domain.model.DashboardSort.NAME) {
            wrapper.orderByAsc(DqHealthScorePO::getAssetName);
        } else {
            wrapper.orderByDesc(DqHealthScorePO::getLastResultAt);
        }
    }

    @Override
    public AssetHealthDetailVO findAssetHealthDetail(String assetId) {
        DqHealthScorePO assetRow = findByAssetAndField(assetId, null);
        if (assetRow == null) {
            return null;
        }
        Instant now = Instant.now();
        AssetHealthDetailVO detail = dqHealthScoreConvertor.toAssetHealthDetailVO(assetRow, now);
        detail.setSourceTool(sourceToolOf(assetRow.getBatchId()));

        List<DqHealthScorePO> fieldPOs = dqHealthScoreRepository.selectList(Wrappers.<DqHealthScorePO>lambdaQuery()
                .eq(DqHealthScorePO::getAssetId, assetId)
                .isNotNull(DqHealthScorePO::getFieldName));
        Map<String, Integer> ruleCounts = ruleCountByField(assetId);
        List<FieldHealthVO> fields = new ArrayList<>(fieldPOs.size());
        for (DqHealthScorePO fieldPo : fieldPOs) {
            FieldHealthVO fieldVo = dqHealthScoreConvertor.toFieldHealthVO(fieldPo, now);
            fieldVo.setRuleCount(ruleCounts.getOrDefault(fieldPo.getFieldName(), 0));
            fields.add(fieldVo);
        }
        detail.setFields(fields);
        return detail;
    }

    @Override
    public RuleDetailVO findRuleDetail(String assetId, String fieldName) {
        String targetField = (fieldName == null || fieldName.trim().isEmpty()) ? null : fieldName.trim();
        DqHealthScorePO scoreRow = findByAssetAndField(assetId, targetField);
        if (scoreRow == null) {
            return null;
        }
        RuleDetailVO vo = dqHealthScoreConvertor.toRuleDetailVO(scoreRow, Instant.now());
        DqBatchPO batch = dqBatchRepository.selectById(scoreRow.getBatchId());
        vo.setBatchNo(batch == null ? null : batch.getBatchNo());

        AlgorithmVO algorithm = new AlgorithmVO();
        algorithm.setFormula(SCORE_FORMULA);
        algorithm.setWeights(toWeightVOs(healthScoreEngine.defaultWeights()));
        vo.setAlgorithm(algorithm);

        List<DqRuleDetailPO> rulePOs = dqRuleDetailRepository.selectList(
                applyTargetField(Wrappers.<DqRuleDetailPO>lambdaQuery()
                        .eq(DqRuleDetailPO::getBatchId, scoreRow.getBatchId())
                        .eq(DqRuleDetailPO::getAssetId, assetId), targetField));
        List<RuleScoreVO> rules = rulePOs.stream()
                .map(dqRuleDetailConvertor::toRuleScoreVO)
                .collect(Collectors.toList());
        vo.setRules(rules);
        return vo;
    }

    private LambdaQueryWrapper<DqRuleDetailPO> applyTargetField(LambdaQueryWrapper<DqRuleDetailPO> wrapper,
            String targetField) {
        if (targetField == null) {
            wrapper.isNull(DqRuleDetailPO::getFieldName);
        } else {
            wrapper.eq(DqRuleDetailPO::getFieldName, targetField);
        }
        return wrapper;
    }

    private void applyBandFilter(LambdaQueryWrapper<DqHealthScorePO> wrapper, BandFilter band) {
        if (band == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        switch (band) {
            case GOOD:
                wrapper.eq(DqHealthScorePO::getHealthBand, "优")
                        .and(w -> w.isNull(DqHealthScorePO::getValidUntil)
                                .or().ge(DqHealthScorePO::getValidUntil, now));
                break;
            case FAIR:
                wrapper.eq(DqHealthScorePO::getHealthBand, "良")
                        .and(w -> w.isNull(DqHealthScorePO::getValidUntil)
                                .or().ge(DqHealthScorePO::getValidUntil, now));
                break;
            case POOR:
                wrapper.eq(DqHealthScorePO::getHealthBand, "差")
                        .and(w -> w.isNull(DqHealthScorePO::getValidUntil)
                                .or().ge(DqHealthScorePO::getValidUntil, now));
                break;
            case EXPIRED:
                wrapper.lt(DqHealthScorePO::getValidUntil, now);
                break;
            case NORESULT:
                // 无结果独立展示态不落 dq_health_score（不归入档位）；筛选命中恒空，以空分页表达
                wrapper.eq(DqHealthScorePO::getId, -1L);
                break;
            default:
                break;
        }
    }

    private DqHealthScorePO findByAssetAndField(String assetId, String fieldName) {
        LambdaQueryWrapper<DqHealthScorePO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqHealthScorePO::getAssetId, assetId);
        if (fieldName == null) {
            wrapper.isNull(DqHealthScorePO::getFieldName);
        } else {
            wrapper.eq(DqHealthScorePO::getFieldName, fieldName);
        }
        wrapper.orderByDesc(DqHealthScorePO::getComputedAt).last("LIMIT 1");
        List<DqHealthScorePO> pos = dqHealthScoreRepository.selectList(wrapper);
        return pos.isEmpty() ? null : pos.get(0);
    }

    private Map<String, DqRuleDetailPO> existingDetails(Long batchId, String assetId, String fieldName) {
        LambdaQueryWrapper<DqRuleDetailPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DqRuleDetailPO::getBatchId, batchId)
                .eq(DqRuleDetailPO::getAssetId, assetId);
        applyTargetField(wrapper, fieldName);
        List<DqRuleDetailPO> pos = dqRuleDetailRepository.selectList(wrapper);
        Map<String, DqRuleDetailPO> byRule = new HashMap<>();
        for (DqRuleDetailPO po : pos) {
            byRule.put(po.getRuleName(), po);
        }
        return byRule;
    }

    private Map<String, Integer> ruleCountByField(String assetId) {
        QueryWrapper<DqRuleDetailPO> wrapper = new QueryWrapper<>();
        wrapper.select("field_name", "COUNT(*) AS cnt")
                .eq("asset_id", assetId)
                .isNotNull("field_name")
                .groupBy("field_name");
        List<Map<String, Object>> rows = dqRuleDetailRepository.selectMaps(wrapper);
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            // 数据库别名大小写差异（H2 / MySQL）容错
            String fieldName = findIgnoreCase(row, "field_name");
            Number cnt = findNumberIgnoreCase(row, "cnt");
            if (fieldName != null && cnt != null) {
                counts.put(fieldName, cnt.intValue());
            }
        }
        return counts;
    }

    private static String findIgnoreCase(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                return String.valueOf(entry.getValue());
            }
        }
        return null;
    }

    private static Number findNumberIgnoreCase(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)
                    && entry.getValue() instanceof Number) {
                return (Number) entry.getValue();
            }
        }
        return null;
    }

    private SourceTool sourceToolOf(Long batchId) {
        if (batchId == null) {
            return null;
        }
        DqBatchPO batch = dqBatchRepository.selectById(batchId);
        return batch == null ? null : SourceTool.fromCodeOrNull(batch.getSourceTool());
    }

    private static List<RuleWeightVO> toWeightVOs(List<RuleWeight> weights) {
        List<RuleWeightVO> vos = new ArrayList<>(weights.size());
        for (RuleWeight weight : weights) {
            RuleWeightVO vo = new RuleWeightVO();
            vo.setRuleName(weight.getRuleName());
            vo.setRuleType(weight.getRuleType());
            vo.setWeight(weight.getWeight());
            vos.add(vo);
        }
        return vos;
    }
}
