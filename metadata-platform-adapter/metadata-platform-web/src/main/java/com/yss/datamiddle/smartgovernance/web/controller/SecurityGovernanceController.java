package com.yss.datamiddle.smartgovernance.web.controller;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.datamiddle.smartgovernance.application.service.SecurityGovernanceApplicationService;
import com.yss.datamiddle.smartgovernance.domain.security.model.ClassificationRule;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityTemplate;
import com.yss.datamiddle.smartgovernance.web.dto.CreateSecurityTemplateDTO;
import com.yss.datamiddle.smartgovernance.web.dto.TriggerSecurityScanDTO;
import com.yss.datamiddle.smartgovernance.web.dto.UpdateSecurityTemplateDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;
import com.yss.datamiddle.smartgovernance.web.dto.BatchApproveCandidatesDTO;

@RestController
@RequiredArgsConstructor
@Api(tags = "智能数据安全分类分级治理")
@RequestMapping("/api/smart-governance/security")
public class SecurityGovernanceController {

    private final SecurityGovernanceApplicationService securityService;

    @GetMapping("/templates")
    @ApiOperation("查询行业合规模板列表")
    public SingleResult<ArrayList<SecurityTemplate>> listTemplates(@RequestParam(required = false) String keyword) {
        List<SecurityTemplate> list = securityService.listTemplates(keyword);
        return SingleResult.of(new ArrayList<>(list));
    }

    @PostMapping("/templates")
    @ApiOperation("创建自定义合规模板")
    public SingleResult<String> createTemplate(@Valid @RequestBody CreateSecurityTemplateDTO dto,
                                              @RequestHeader(value = "X-User-Id", defaultValue = "sec_admin") String operator) {
        SecurityTemplate template = SecurityTemplate.builder()
                .templateCode(dto.getTemplateCode())
                .templateName(dto.getTemplateName())
                .standardAuthority(dto.getStandardAuthority())
                .description(dto.getDescription())
                .defaultAutoApproval(dto.getDefaultAutoApproval())
                .defaultThreshold(dto.getDefaultThreshold())
                .isSystemBuiltIn(false)
                .isActive(true)
                .createdBy(operator)
                .updatedBy(operator)
                .build();

        List<ClassificationRule> rules = new ArrayList<>();
        if (dto.getRules() != null) {
            for (CreateSecurityTemplateDTO.ClassificationRuleDTO r : dto.getRules()) {
                rules.add(ClassificationRule.builder()
                        .sensitiveType(r.getSensitiveType())
                        .sensitiveName(r.getSensitiveName())
                        .securityLevel(SecurityLevel.of(r.getSecurityLevel()))
                        .clauseRef(r.getClauseRef())
                        .regexPattern(r.getRegexPattern())
                        .dictionaryWords(r.getDictionaryWords())
                        .semanticPrompt(r.getSemanticPrompt())
                        .isActive(r.getIsActive() != null ? r.getIsActive() : true)
                        .priority(r.getPriority() != null ? r.getPriority() : 100)
                        .createdBy(operator)
                        .updatedBy(operator)
                        .build());
            }
        }

        String id = securityService.createTemplate(template, rules);
        return SingleResult.of(id);
    }

    @GetMapping("/templates/{id}")
    @ApiOperation("获取指定合规模板详情与规则项")
    public SingleResult<SecurityTemplate> getTemplateDetail(@PathVariable String id) {
        return SingleResult.of(securityService.getTemplateDetail(id).orElse(null));
    }

    @PutMapping("/templates/{id}")
    @ApiOperation("更新合规模板配置与自动打标阈值")
    public SingleResult<Boolean> updateTemplate(@PathVariable String id, @Valid @RequestBody UpdateSecurityTemplateDTO dto) {
        SecurityTemplate update = SecurityTemplate.builder()
                .templateName(dto.getTemplateName())
                .description(dto.getDescription())
                .defaultAutoApproval(dto.getDefaultAutoApproval())
                .defaultThreshold(dto.getDefaultThreshold())
                .isActive(dto.getIsActive())
                .build();
        securityService.updateTemplate(id, update);
        return SingleResult.of(true);
    }

    @PostMapping("/scan")
    @ApiOperation("触发数据安全三层漏斗分类分级扫描")
    public SingleResult<String> triggerScan(@Valid @RequestBody(required = false) TriggerSecurityScanDTO dto) {
        String taskId = securityService.triggerScan(
                dto != null ? dto.getTemplateId() : null,
                dto != null ? dto.getDataSource() : null,
                dto != null ? dto.getDatabaseName() : null,
                dto != null ? dto.getTableName() : null
        );
        return SingleResult.of(taskId);
    }

    @GetMapping("/candidates")
    @ApiOperation("查询安全打标候选池列表")
    public com.yss.cloud.dto.result.PageResult<SensitiveCandidate> listCandidates(
            @RequestParam(defaultValue = "1") Integer pageIndex,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) SecurityLevel securityLevel,
            @RequestParam(required = false) String sensitiveType,
            @RequestParam(required = false) String keyword) {
        List<SensitiveCandidate> list = securityService.queryCandidates(pageIndex, pageSize, status, securityLevel, sensitiveType, keyword);
        long total = securityService.countCandidates(status, securityLevel, sensitiveType, keyword);
        return com.yss.cloud.dto.result.PageResult.of(list, (int) total, pageSize, pageIndex);
    }

    @PostMapping("/candidates/batch-approve")
    @ApiOperation("批量采纳候选打标")
    public SingleResult<java.util.HashMap<String, Integer>> batchApprove(
            @RequestBody BatchApproveCandidatesDTO dto,
            @RequestHeader(value = "X-User-Id", defaultValue = "sec_admin") String operator) {
        Map<String, Integer> result = securityService.batchApprove(dto.getCandidateIds(), operator);
        return SingleResult.of(new java.util.HashMap<>(result));
    }
}

