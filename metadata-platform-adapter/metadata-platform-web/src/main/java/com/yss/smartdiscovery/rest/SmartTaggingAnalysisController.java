package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.BatchSummaryDTO;
import com.yss.smartdiscovery.application.service.TaggingFunnelAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-discovery/tagging")
@RequiredArgsConstructor
public class SmartTaggingAnalysisController {

    private final TaggingFunnelAppService taggingFunnelAppService;

    @PostMapping("/analyze")
    public SingleResult<BatchSummaryDTO> analyzeTags(@RequestBody(required = false) Map<String, Object> request) {
        List<String> tableNames = request != null && request.containsKey("tableNames") ? (List<String>) request.get("tableNames") : Collections.emptyList();
        String domain = request != null && request.containsKey("domainFilter") ? (String) request.get("domainFilter") : null;
        return SingleResult.of(taggingFunnelAppService.runTaggingAnalysis(tableNames, domain));
    }
}
