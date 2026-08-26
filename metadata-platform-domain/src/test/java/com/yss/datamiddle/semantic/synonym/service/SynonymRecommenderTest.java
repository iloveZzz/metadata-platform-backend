package com.yss.datamiddle.semantic.synonym.service;

import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SynonymRecommenderTest {

    private final SynonymRecommender recommender = new SynonymRecommender();

    @Test
    public void testSubstringContainRecommendation() {
        Set<String> pool = new HashSet<>(Arrays.asList("营业收入", "净利润", "主营收入", "销售额", "无相关词"));
        List<SynonymRecommendation> list = recommender.recommend("营收", pool, 3);

        Assertions.assertFalse(list.isEmpty());
        Assertions.assertTrue(list.stream().anyMatch(r -> r.getCandidateWord().equals("营业收入")));
        Assertions.assertTrue(list.stream().anyMatch(r -> r.getCandidateWord().equals("主营收入")));
        Assertions.assertTrue(list.get(0).getSimilarityScore() >= 0.80);
    }

    @Test
    public void testPrefixAndSuffixMatch() {
        Set<String> pool = new HashSet<>(Arrays.asList("用户总数", "活跃用户数", "付费用户数", "不相关"));
        List<SynonymRecommendation> list = recommender.recommend("用户", pool, 5);

        Assertions.assertTrue(list.size() >= 3);
        Assertions.assertTrue(list.stream().anyMatch(r -> r.getCandidateWord().equals("用户总数")));
    }

    @Test
    public void testEditDistanceSimilarity() {
        Set<String> pool = new HashSet<>(Arrays.asList("GMV总量", "GTV总量", "完全无关"));
        List<SynonymRecommendation> list = recommender.recommend("GMV总额", pool, 2);

        Assertions.assertFalse(list.isEmpty());
        Assertions.assertEquals("GMV总量", list.get(0).getCandidateWord());
        Assertions.assertTrue(list.get(0).getSimilarityScore() >= 0.6);
    }
}
