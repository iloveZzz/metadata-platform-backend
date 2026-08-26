package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.QueryExpansionResult;
import com.yss.datamiddle.semantic.application.model.SynonymExpansionItem;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索同义词展开服务（SL-005 / SB-05）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchExpansionService {

    private final SynonymSetGateway synonymSetGateway;

    public List<QueryExpansionResult> expand(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "queries", "REQUIRED", "查询词列表不能为空");
        }

        List<SynonymSet> activeSets = synonymSetGateway.listAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                .collect(Collectors.toList());

        List<QueryExpansionResult> results = new ArrayList<>();

        for (String query : queries) {
            if (query == null || query.trim().isEmpty()) {
                results.add(QueryExpansionResult.builder()
                        .query(query)
                        .expansions(Collections.emptyList())
                        .build());
                continue;
            }

            String trimmed = query.trim();
            List<SynonymExpansionItem> matchedExpansions = activeSets.stream()
                    .filter(s -> s.getWords().stream().anyMatch(w -> w.equalsIgnoreCase(trimmed)))
                    .map(s -> SynonymExpansionItem.builder()
                            .synonymSetId(s.getId())
                            .name(s.getName())
                            .canonical(s.getCanonical())
                            .words(new ArrayList<>(s.getWords()))
                            .termId(s.getTermId())
                            .build())
                    .collect(Collectors.toList());

            results.add(QueryExpansionResult.builder()
                    .query(query)
                    .expansions(matchedExpansions)
                    .build());
        }

        return results;
    }
}
