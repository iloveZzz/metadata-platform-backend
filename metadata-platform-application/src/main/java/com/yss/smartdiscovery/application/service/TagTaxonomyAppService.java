package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.application.dto.SandboxResultDTO;
import com.yss.smartdiscovery.application.dto.TagDTO;
import com.yss.smartdiscovery.application.dto.TagRuleDTO;
import com.yss.smartdiscovery.domain.gateway.TagRepository;
import com.yss.smartdiscovery.domain.rule.SandboxTester;
import com.yss.smartdiscovery.domain.rule.TagRule;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagTaxonomyAppService {

    private final TagRepository tagRepository;

    public List<TagDTO> listTags(String categoryCode) {
        return tagRepository.listTags(categoryCode).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TagDTO getTagById(String id) {
        return tagRepository.findTagById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在: " + id));
    }

    public TagDTO createTag(TagDTO createDTO) {
        SmartTagDefinition tag = SmartTagDefinition.builder()
                .id("TAG-" + UUID.randomUUID().toString().substring(0, 8))
                .tagName(createDTO.getName())
                .tagCode(createDTO.getCode())
                .categoryCode(createDTO.getCategoryCode())
                .categoryName(createDTO.getCategoryName())
                .colorToken(createDTO.getColorToken() != null ? createDTO.getColorToken() : "blue")
                .description(createDTO.getDescription())
                .isEnabled(true)
                .tagRule(TagRule.builder()
                        .regexPattern("^(" + createDTO.getCode().toLowerCase() + ")")
                        .boundTermNames(Collections.singletonList(createDTO.getName()))
                        .fewShotPrompt("识别属于 " + createDTO.getName() + " 相关的属性。")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tag.validate();
        return toDTO(tagRepository.saveTag(tag));
    }

    public TagDTO updateTag(String id, TagDTO updateDTO) {
        SmartTagDefinition existing = tagRepository.findTagById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在: " + id));
        if (updateDTO.getName() != null) existing.setTagName(updateDTO.getName());
        if (updateDTO.getColorToken() != null) existing.setColorToken(updateDTO.getColorToken());
        if (updateDTO.getDescription() != null) existing.setDescription(updateDTO.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.validate();
        return toDTO(tagRepository.saveTag(existing));
    }

    public void deleteTag(String id) {
        tagRepository.deleteTag(id);
    }

    public TagRuleDTO getTagRules(String tagId) {
        SmartTagDefinition tag = tagRepository.findTagById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在: " + tagId));
        TagRule rule = tag.getTagRule();
        if (rule == null) {
            rule = TagRule.builder().tagId(tagId).build();
        }
        return TagRuleDTO.builder()
                .tagId(tagId)
                .regexPattern(rule.getRegexPattern())
                .boundTermIds(rule.getBoundTermIds())
                .boundTermNames(rule.getBoundTermNames())
                .fewShotPrompt(rule.getFewShotPrompt())
                .scopeFilter(rule.getScopeFilter())
                .build();
    }

    public TagRuleDTO updateTagRules(String tagId, TagRuleDTO ruleDTO) {
        SmartTagDefinition tag = tagRepository.findTagById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在: " + tagId));
        TagRule rule = TagRule.builder()
                .id(UUID.randomUUID().toString())
                .tagId(tagId)
                .regexPattern(ruleDTO.getRegexPattern())
                .boundTermIds(ruleDTO.getBoundTermIds())
                .boundTermNames(ruleDTO.getBoundTermNames())
                .fewShotPrompt(ruleDTO.getFewShotPrompt())
                .scopeFilter(ruleDTO.getScopeFilter())
                .updatedAt(LocalDateTime.now())
                .build();
        rule.validate();
        tag.setTagRule(rule);
        tagRepository.saveTag(tag);
        return ruleDTO;
    }

    public SandboxResultDTO testSandboxRule(String fieldName, String fieldComment) {
        List<SmartTagDefinition> availableTags = tagRepository.listTags(null);
        SandboxTester.SandboxResult result = SandboxTester.testField(fieldName, fieldComment, availableTags);
        return SandboxResultDTO.builder()
                .matchedTagId(result.getMatchedTagId())
                .matchedTagName(result.getMatchedTagName())
                .confidence(result.getConfidence())
                .explanation(result.getExplanation())
                .l1RegexHit(result.getL1RegexHit())
                .l2GlossaryHit(result.getL2GlossaryHit())
                .l3LlmHit(result.getL3LlmHit())
                .build();
    }

    private TagDTO toDTO(SmartTagDefinition entity) {
        return TagDTO.builder()
                .id(entity.getId())
                .name(entity.getTagName())
                .code(entity.getTagCode())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .colorToken(entity.getColorToken())
                .description(entity.getDescription())
                .isEnabled(entity.getIsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
