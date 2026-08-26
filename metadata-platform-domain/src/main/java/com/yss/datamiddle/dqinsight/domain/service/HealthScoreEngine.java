package com.yss.datamiddle.dqinsight.domain.service;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.RuleWeight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康分内部规则引擎（HealthScoreEngine，Domain 领域服务，系统概要 §2/§5）。
 *
 * <p>健康分 = Σ(规则权重 × 规则得分)，规则得分 passed=100 / warn=80 / failed|error=0（SB-02 已确认）；
 * MVP 固定默认权重（非空率 0.25 / 格式 0.25 / 唯一性 0.20 / 值域 0.15 / 新鲜度 0.15，权重合计为 1，配置化 P1）；
 * 档位映射 ≥90 优 / 75~89 良 / &lt;75 差（OQ-01 已确认）。同一规则类型多条规则时取平均分后加权（透明可解释，
 * 单类型未接入按 0 分计、权重保留，公式恒等）。ruleVersion 随每次计算递增。</p>
 */
public class HealthScoreEngine {

    /** 档位阈值（OQ-01 已确认；配置化 P1） */
    public static final int BAND_GOOD_MIN = 90;
    public static final int BAND_FAIR_MIN = 75;

    /** 规则得分映射（SB-02：passed=100 / warn=80 / failed|error=0） */
    public static final int SCORE_PASSED = 100;
    public static final int SCORE_WARN = 80;
    public static final int SCORE_FAILED = 0;

    /** 默认权重合计（C22：权重合计必须为 1） */
    public static final double WEIGHT_TOTAL = 1.0d;

    /** 默认规则清单默认名（分数来源区展示） */
    private static final String DEFAULT_RULE_NAME_NON_NULL = "非空率";
    private static final String DEFAULT_RULE_NAME_FORMAT = "格式";
    private static final String DEFAULT_RULE_NAME_UNIQUENESS = "唯一性";
    private static final String DEFAULT_RULE_NAME_VALUE_RANGE = "值域";
    private static final String DEFAULT_RULE_NAME_FRESHNESS = "新鲜度";

    /** MVP 固定默认加权系数（OQ-02 / SB-02 已确认，配置化 P1；C22 权重合计为 1） */
    private static final Map<RuleType, Double> DEFAULT_WEIGHTS;
    static {
        Map<RuleType, Double> weights = new LinkedHashMap<>();
        weights.put(RuleType.NON_NULL_RATE, 0.25d);
        weights.put(RuleType.FORMAT, 0.25d);
        weights.put(RuleType.UNIQUENESS, 0.20d);
        weights.put(RuleType.VALUE_RANGE, 0.15d);
        weights.put(RuleType.FRESHNESS, 0.15d);
        DEFAULT_WEIGHTS = Collections.unmodifiableMap(weights);
    }

    /**
     * 默认权重清单（分数来源区展示：规则名 / 类型 / 权重）。
     */
    public List<RuleWeight> defaultWeights() {
        List<RuleWeight> list = new ArrayList<>(DEFAULT_WEIGHTS.size());
        for (Map.Entry<RuleType, Double> entry : DEFAULT_WEIGHTS.entrySet()) {
            list.add(RuleWeight.builder()
                    .ruleName(defaultRuleName(entry.getKey()))
                    .ruleType(entry.getKey())
                    .weight(entry.getValue())
                    .build());
        }
        return list;
    }

    /**
     * 指定规则类型的默认权重（未定义类型按 0 计）。
     */
    public double weightOf(RuleType ruleType) {
        Double weight = DEFAULT_WEIGHTS.get(ruleType);
        return weight == null ? 0d : weight;
    }

    /**
     * 规则得分映射（passed=100 / warn=80 / failed|error=0，SB-02）。
     */
    public int scoreOf(RuleStatus status) {
        if (status == null) {
            return SCORE_FAILED;
        }
        switch (status) {
            case PASSED:
                return SCORE_PASSED;
            case WARN:
                return SCORE_WARN;
            case FAILED:
            case ERROR:
            default:
                return SCORE_FAILED;
        }
    }

    /**
     * 规则加权计算：健康分 = Σ(规则权重 × 规则得分)。
     *
     * <p>同一规则类型多条规则取平均分后乘权重；该类型未接入时按 0 分计（权重保留，公式恒等）；
     * 结果四舍五入为 0~100 整数。</p>
     */
    public int computeScore(List<RuleResultRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<RuleType, List<Integer>> scoresByType = new EnumMap<>(RuleType.class);
        for (RuleResultRow row : rows) {
            if (row.getRuleType() == null || row.getStatus() == null) {
                continue;
            }
            scoresByType.computeIfAbsent(row.getRuleType(), k -> new ArrayList<>())
                    .add(scoreOf(row.getStatus()));
        }
        double total = 0d;
        for (Map.Entry<RuleType, Double> entry : DEFAULT_WEIGHTS.entrySet()) {
            List<Integer> scores = scoresByType.get(entry.getKey());
            double average = (scores == null || scores.isEmpty()) ? 0d : average(scores);
            total += entry.getValue() * average;
        }
        return (int) Math.round(total);
    }

    /**
     * 档位映射（OQ-01 已确认）：≥90 优 / 75~89 良 / &lt;75 差。
     */
    public HealthBand bandOf(int score) {
        if (score >= BAND_GOOD_MIN) {
            return HealthBand.GOOD;
        }
        if (score >= BAND_FAIR_MIN) {
            return HealthBand.FAIR;
        }
        return HealthBand.POOR;
    }

    /**
     * 规则通过率（'80%' 形态）：passed 数 ÷ 总规则数 × 100%。
     */
    public String passRate(List<RuleResultRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "0%";
        }
        int passed = 0;
        for (RuleResultRow row : rows) {
            if (row.getStatus() == RuleStatus.PASSED) {
                passed++;
            }
        }
        return Math.round(passed * 100.0d / rows.size()) + "%";
    }

    /**
     * 计算规则版本递增（v1 → v2；无历史版本为 v1）。
     */
    public String nextVersion(String latestVersion) {
        if (latestVersion == null || latestVersion.trim().isEmpty()) {
            return "v1";
        }
        String trimmed = latestVersion.trim();
        if (!trimmed.startsWith("v")) {
            return "v1";
        }
        try {
            int number = Integer.parseInt(trimmed.substring(1));
            return "v" + (number + 1);
        } catch (NumberFormatException e) {
            return "v1";
        }
    }

    private static double average(List<Integer> scores) {
        int sum = 0;
        for (int score : scores) {
            sum += score;
        }
        return sum / (double) scores.size();
    }

    private static String defaultRuleName(RuleType ruleType) {
        switch (ruleType) {
            case NON_NULL_RATE:
                return DEFAULT_RULE_NAME_NON_NULL;
            case FORMAT:
                return DEFAULT_RULE_NAME_FORMAT;
            case UNIQUENESS:
                return DEFAULT_RULE_NAME_UNIQUENESS;
            case VALUE_RANGE:
                return DEFAULT_RULE_NAME_VALUE_RANGE;
            case FRESHNESS:
                return DEFAULT_RULE_NAME_FRESHNESS;
            default:
                return ruleType == null ? null : ruleType.getCode();
        }
    }
}
