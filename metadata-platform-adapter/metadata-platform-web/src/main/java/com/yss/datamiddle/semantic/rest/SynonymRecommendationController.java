package com.yss.datamiddle.semantic.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.datamiddle.semantic.application.service.SynonymRecommendationService;
import com.yss.datamiddle.semantic.client.dto.cmd.SynonymAcceptCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.SynonymRecommendationCmd;
import com.yss.datamiddle.semantic.client.vo.SynonymRecommendationVO;
import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能同义词推荐与采纳控制器
 */
@RestController
@RequestMapping("/api/semantic/synonyms/recommend")
@RequiredArgsConstructor
public class SynonymRecommendationController {

    private final SynonymRecommendationService recommendationService;

    @PostMapping
    public MultiResult<SynonymRecommendationVO> recommend(
            @Valid @RequestBody SynonymRecommendationCmd cmd
    ) {
        List<SynonymRecommendation> list = recommendationService.recommendSynonyms(
                cmd.getTargetWord(),
                cmd.getLimit() != null ? cmd.getLimit() : 5
        );

        if (list == null || list.isEmpty()) {
            return MultiResult.of(Collections.emptyList());
        }

        List<SynonymRecommendationVO> vos = list.stream().map(r -> SynonymRecommendationVO.builder()
                .candidateWord(r.getCandidateWord())
                .similarityScore(r.getSimilarityScore())
                .matchReason(r.getMatchReason())
                .build()).collect(Collectors.toList());

        return MultiResult.of(vos);
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(
            @Valid @RequestBody SynonymAcceptCmd cmd
    ) {
        recommendationService.acceptRecommendation(cmd.getSynonymSetId(), cmd.getCandidateWord());
    }
}
