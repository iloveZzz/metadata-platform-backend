package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.SensitiveRuleConvertor;
import com.yss.datasecurity.application.dto.RuleSimulationRequestDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleCreateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleUpdateDTO;
import com.yss.datasecurity.application.dto.SensitiveRuleVO;
import com.yss.datasecurity.application.dto.SimulationFieldMatchVO;
import com.yss.datasecurity.application.service.SensitiveRuleAppService;
import com.yss.datasecurity.domain.enums.CommonStatusEnum;
import com.yss.datasecurity.domain.exception.DataSecurityErrorCode;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.SensitiveRuleGateway;
import com.yss.datasecurity.domain.model.SensitiveRule;
import com.yss.datasecurity.domain.model.SimulationFieldMatch;
import com.yss.datasecurity.domain.service.SensitiveRecognitionSimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveRuleAppServiceImpl implements SensitiveRuleAppService {

    private final SensitiveRuleGateway sensitiveRuleGateway;
    private final SensitiveRecognitionSimulationEngine simulationEngine;
    private final SensitiveRuleConvertor convertor;

    @Override
    public PageResult<SensitiveRuleVO> pageRules(int pageIndex, int pageSize, String keyword, String status, String scanScopeType, String ruleType) {
        List<SensitiveRule> list = sensitiveRuleGateway.pageRules(pageIndex, pageSize, keyword, status, scanScopeType, ruleType);
        long total = sensitiveRuleGateway.countRules(keyword, status, scanScopeType, ruleType);
        List<SensitiveRuleVO> voList = convertor.toSummaryVOList(list);
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    public SensitiveRuleVO getDetail(Long id) {
        SensitiveRule rule = sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "识别特征不存在: " + id));
        return convertor.toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SensitiveRuleCreateDTO dto) {
        sensitiveRuleGateway.findByName(dto.getRuleName()).ifPresent(r -> {
            throw new DataSecurityException(DataSecurityErrorCode.RULE_NAME_DUPLICATE, "特征名称已存在: " + dto.getRuleName());
        });

        SensitiveRule domain = convertor.toDomain(dto);
        if (domain.getRuleType() == null || domain.getRuleType().trim().isEmpty()) {
            domain.setRuleType("CUSTOM");
        }
        domain.validatePriority();
        SensitiveRule saved = sensitiveRuleGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SensitiveRuleUpdateDTO dto) {
        SensitiveRule rule = sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "识别特征不存在: " + id));

        sensitiveRuleGateway.findByName(dto.getRuleName()).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new DataSecurityException(DataSecurityErrorCode.RULE_NAME_DUPLICATE, "特征名称已存在: " + dto.getRuleName());
            }
        });

        convertor.updateDomainFromDTO(dto, rule);
        rule.validatePriority();
        sensitiveRuleGateway.update(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SensitiveRule rule = sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "识别特征不存在: " + id));
        if ("BUILTIN".equalsIgnoreCase(rule.getRuleType())) {
            throw new DataSecurityException(DataSecurityErrorCode.BUILTIN_RULE_CANNOT_DELETE, "内置识别特征受系统保护，不可删除");
        }
        sensitiveRuleGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "识别特征不存在: " + id));
        sensitiveRuleGateway.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cloneRule(Long id) {
        SensitiveRule source = sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "待克隆特征不存在: " + id));

        String cloneName = source.getRuleName() + "_COPY";
        SensitiveRule clone = SensitiveRule.builder()
            .ruleName(cloneName)
            .ruleType("CUSTOM") // 克隆生成的规则均为自定义特征
            .description("克隆自: " + source.getRuleName() + (source.getDescription() != null ? " - " + source.getDescription() : ""))
            .priority(source.getPriority())
            .owner(source.getOwner())
            .status(CommonStatusEnum.ENABLED.getCode())
            .categoryScopeMode(source.getCategoryScopeMode())
            .categoryScopeIds(source.getCategoryScopeIds())
            .scanScopeType(source.getScanScopeType())
            .scanScopeConfig(source.getScanScopeConfig())
            .featureConfig(source.getFeatureConfig())
            .taggedFieldsCount(0)
            .build();

        SensitiveRule saved = sensitiveRuleGateway.save(clone);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetRule(Long id) {
        sensitiveRuleGateway.findById(id)
            .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RULE_NOT_FOUND, "敏感识别规则不存在: " + id));
        sensitiveRuleGateway.clearTaggedFields(id);
    }

    @Override
    public List<SimulationFieldMatchVO> simulate(RuleSimulationRequestDTO dto) {
        String fieldRegex = null;
        String contentRegex = null;
        Double threshold = 0.8;

        if (dto.getRuleDraftConfig() != null && dto.getRuleDraftConfig().getFeatureConfig() != null) {
            String configStr = dto.getRuleDraftConfig().getFeatureConfig().toString();
            if (configStr.contains("fieldNameRegex")) {
                fieldRegex = extractJsonValue(configStr, "fieldNameRegex");
            }
            if (configStr.contains("contentSampleRegex")) {
                contentRegex = extractJsonValue(configStr, "contentSampleRegex");
            }
        }

        List<SimulationFieldMatch> matches = simulationEngine.simulate(
            dto.getDatasourceId(),
            dto.getTableNames(),
            fieldRegex,
            contentRegex,
            threshold
        );

        return convertor.toSimulationVOList(matches);
    }

    private String extractJsonValue(String text, String key) {
        try {
            int idx = text.indexOf(key);
            if (idx >= 0) {
                int start = text.indexOf(":", idx) + 1;
                int end = text.indexOf(",", start);
                if (end < 0) end = text.indexOf("}", start);
                if (end < 0) end = text.length();
                return text.substring(start, end).replace("\"", "").trim();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
