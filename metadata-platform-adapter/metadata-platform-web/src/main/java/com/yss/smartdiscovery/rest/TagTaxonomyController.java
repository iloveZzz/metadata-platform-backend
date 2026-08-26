package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.Result;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.SandboxResultDTO;
import com.yss.smartdiscovery.application.dto.TagDTO;
import com.yss.smartdiscovery.application.dto.TagRuleDTO;
import com.yss.smartdiscovery.application.service.TagTaxonomyAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-discovery/tags")
@RequiredArgsConstructor
public class TagTaxonomyController {

    private final TagTaxonomyAppService tagTaxonomyAppService;

    @GetMapping
    public MultiResult<TagDTO> listTags(@RequestParam(required = false) String categoryCode) {
        return MultiResult.of(tagTaxonomyAppService.listTags(categoryCode));
    }

    @PostMapping
    public SingleResult<TagDTO> createTag(@Valid @RequestBody TagDTO createDTO) {
        return SingleResult.of(tagTaxonomyAppService.createTag(createDTO));
    }

    @GetMapping("/{id}")
    public SingleResult<TagDTO> getTagById(@PathVariable String id) {
        return SingleResult.of(tagTaxonomyAppService.getTagById(id));
    }

    @PutMapping("/{id}")
    public SingleResult<TagDTO> updateTag(@PathVariable String id, @Valid @RequestBody TagDTO updateDTO) {
        return SingleResult.of(tagTaxonomyAppService.updateTag(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    public Result deleteTag(@PathVariable String id) {
        tagTaxonomyAppService.deleteTag(id);
        return Result.buildSuccess();
    }

    @GetMapping("/{id}/rules")
    public SingleResult<TagRuleDTO> getTagRules(@PathVariable String id) {
        return SingleResult.of(tagTaxonomyAppService.getTagRules(id));
    }

    @PutMapping("/{id}/rules")
    public SingleResult<TagRuleDTO> updateTagRules(@PathVariable String id, @Valid @RequestBody TagRuleDTO ruleDTO) {
        return SingleResult.of(tagTaxonomyAppService.updateTagRules(id, ruleDTO));
    }

    @PostMapping("/sandbox-test")
    public SingleResult<SandboxResultDTO> testSandboxRule(@RequestBody Map<String, String> request) {
        String fieldName = request.get("fieldName");
        String fieldComment = request.get("fieldComment");
        return SingleResult.of(tagTaxonomyAppService.testSandboxRule(fieldName, fieldComment));
    }
}
