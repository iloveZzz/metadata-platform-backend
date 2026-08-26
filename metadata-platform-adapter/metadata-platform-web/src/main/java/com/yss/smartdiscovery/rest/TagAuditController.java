package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.Result;
import com.yss.smartdiscovery.application.dto.TagAuditLogDTO;
import com.yss.smartdiscovery.application.service.CandidatePoolAppService;
import com.yss.smartdiscovery.application.service.TagAuditAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/smart-discovery/tagging/audit")
@RequiredArgsConstructor
public class TagAuditController {

    private final TagAuditAppService tagAuditAppService;
    private final CandidatePoolAppService candidatePoolAppService;

    @GetMapping
    public PageResult<TagAuditLogDTO> listAuditLogs(
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<TagAuditLogDTO> list = tagAuditAppService.listAuditLogs();
        return PageResult.of(list, list.size(), pageSize, pageIndex);
    }

    @PostMapping("/{batchId}/rollback")
    public Result rollbackBatch(@PathVariable String batchId) {
        candidatePoolAppService.rollbackBatch(batchId);
        return Result.buildSuccess();
    }
}
