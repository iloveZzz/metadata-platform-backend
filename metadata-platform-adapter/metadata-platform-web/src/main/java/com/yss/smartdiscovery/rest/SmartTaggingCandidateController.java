package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.BatchSummaryDTO;
import com.yss.smartdiscovery.application.dto.TagCandidateDTO;
import com.yss.smartdiscovery.application.service.CandidatePoolAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-discovery/tagging/candidates")
@RequiredArgsConstructor
public class SmartTaggingCandidateController {

    private final CandidatePoolAppService candidatePoolAppService;

    @GetMapping
    public PageResult<TagCandidateDTO> listCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<TagCandidateDTO> list = candidatePoolAppService.listCandidates(status, source, domain);
        return PageResult.of(list, list.size(), pageSize, pageIndex);
    }

    @PostMapping("/batch-approve")
    public SingleResult<BatchSummaryDTO> batchApprove(@RequestBody Map<String, Object> request) {
        List<String> ids = (List<String>) request.get("candidateIds");
        String reason = (String) request.get("reason");
        return SingleResult.of(candidatePoolAppService.batchApprove(ids, reason));
    }

    @PostMapping("/batch-reject")
    public SingleResult<BatchSummaryDTO> batchReject(@RequestBody Map<String, Object> request) {
        List<String> ids = (List<String>) request.get("candidateIds");
        String reason = (String) request.get("reason");
        return SingleResult.of(candidatePoolAppService.batchReject(ids, reason));
    }

    @PostMapping("/{id}/modify")
    public SingleResult<TagCandidateDTO> modifyAndApprove(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String targetTag = request.get("targetTag");
        String reason = request.get("modifyReason");
        return SingleResult.of(candidatePoolAppService.modifyAndApprove(id, targetTag, reason));
    }

    @GetMapping("/drawer")
    public MultiResult<TagCandidateDTO> getDrawerSuggestions(@RequestParam(required = false) String tableName) {
        return MultiResult.of(candidatePoolAppService.getAssetDrawerSuggestions(tableName));
    }
}
