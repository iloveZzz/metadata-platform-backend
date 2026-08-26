package com.yss.datamiddle.semantic.synonym.service;

import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 智能同义词与近义词推荐领域计算引擎
 */
public class SynonymRecommender {

    /**
     * 从候选语料池中针对目标词计算推荐列表
     *
     * @param targetWord 目标词
     * @param candidates 候选语料池
     * @param limit      最大返回条数
     * @return 推荐列表（按得分降序）
     */
    public List<SynonymRecommendation> recommend(String targetWord, Set<String> candidates, int limit) {
        if (targetWord == null || targetWord.trim().isEmpty() || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String target = targetWord.trim();
        List<SynonymRecommendation> results = new ArrayList<>();

        for (String candidate : candidates) {
            if (candidate == null || candidate.trim().isEmpty()) {
                continue;
            }
            String cand = candidate.trim();
            if (cand.equalsIgnoreCase(target)) {
                continue; // 相同词忽略
            }

            double score = 0.0;
            String reason = "EDIT_DISTANCE";

            if (cand.contains(target) || target.contains(cand)) {
                score = 0.85 + (0.15 * Math.min(cand.length(), target.length()) / Math.max(cand.length(), target.length()));
                reason = "SUBSTRING_CONTAIN";
            } else if (isSubsequence(target, cand) || isSubsequence(cand, target)) {
                score = 0.82;
                reason = "SUBSEQUENCE_MATCH";
            } else if (cand.startsWith(target) || target.startsWith(cand)) {
                score = 0.80;
                reason = "PREFIX_MATCH";
            } else if (cand.endsWith(target) || target.endsWith(cand)) {
                score = 0.75;
                reason = "SUFFIX_MATCH";
            } else {
                double editSim = computeNormalizedLevenshtein(target, cand);
                if (editSim >= 0.4) {
                    score = editSim;
                    reason = "EDIT_DISTANCE";
                }
            }

            if (score >= 0.4) {
                results.add(SynonymRecommendation.builder()
                        .candidateWord(cand)
                        .similarityScore(Math.round(score * 100.0) / 100.0)
                        .matchReason(reason)
                        .build());
            }
        }

        results.sort(Comparator.comparingDouble(SynonymRecommendation::getSimilarityScore).reversed());

        if (limit > 0 && results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    private boolean isSubsequence(String sub, String str) {
        if (sub == null || str == null) return false;
        int i = 0, j = 0;
        while (i < sub.length() && j < str.length()) {
            if (sub.charAt(i) == str.charAt(j)) {
                i++;
            }
            j++;
        }
        return i == sub.length();
    }

    private double computeNormalizedLevenshtein(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
