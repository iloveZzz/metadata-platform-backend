package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import com.yss.datamiddle.semantic.synonym.service.SynonymRecommender;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.model.Term;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 同义词智能推荐与采纳应用服务
 */
@Service
public class SynonymRecommendationService {

    private final SynonymSetGateway synonymSetGateway;
    private final TermGateway termGateway;
    private final CurrentUserPort currentUserPort;
    private final SynonymRecommender recommender = new SynonymRecommender();

    public SynonymRecommendationService(
            SynonymSetGateway synonymSetGateway,
            TermGateway termGateway,
            CurrentUserPort currentUserPort
    ) {
        this.synonymSetGateway = synonymSetGateway;
        this.termGateway = termGateway;
        this.currentUserPort = currentUserPort;
    }

    /**
     * 针对目标词计算推荐同义词候选
     */
    @Transactional(readOnly = true)
    public List<SynonymRecommendation> recommendSynonyms(String targetWord, int limit) {
        Set<String> candidatePool = new HashSet<>();

        // 1. 从术语词库收集词条名称与别名
        List<Term> allTerms = termGateway.listAll();
        if (allTerms != null) {
            for (Term term : allTerms) {
                if (term.getName() != null) candidatePool.add(term.getName());
                if (term.getAliases() != null) candidatePool.addAll(term.getAliases());
            }
        }

        // 2. 从已有同义词组收集词条
        List<SynonymSet> allSets = synonymSetGateway.listAll();
        if (allSets != null) {
            for (SynonymSet set : allSets) {
                if (set.getCanonical() != null) candidatePool.add(set.getCanonical());
                if (set.getWords() != null) candidatePool.addAll(set.getWords());
            }
        }

        return recommender.recommend(targetWord, candidatePool, limit > 0 ? limit : 5);
    }

    /**
     * 一键采纳推荐词到指定的同义词组
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptRecommendation(Long synonymSetId, String candidateWord) {
        if (!currentUserPort.isWritePermitted()) {
            throw new PermissionDeniedException("无权执行同义词采纳操作");
        }

        SynonymSet synonymSet = synonymSetGateway.findById(synonymSetId)
                .orElseThrow(() -> new IllegalArgumentException("同义词组不存在: " + synonymSetId));

        synonymSet.addWord(candidateWord);
        synonymSetGateway.update(synonymSet);
    }
}
