package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.AskResponseDTO;
import com.yss.smartdiscovery.application.service.AskMetadataAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController("smartDiscoveryAskMetadataController")
@RequestMapping("/api/smart-discovery/ask")
@RequiredArgsConstructor
public class AskMetadataController {

    private final AskMetadataAppService askMetadataAppService;

    @PostMapping
    public SingleResult<AskResponseDTO> askMetadata(@RequestBody Map<String, String> request) {
        String queryText = request.get("queryText");
        return SingleResult.of(askMetadataAppService.askMetadata(queryText));
    }

    @GetMapping("/suggestions")
    public MultiResult<String> getSuggestions() {
        return MultiResult.of(askMetadataAppService.getSuggestions());
    }
}
