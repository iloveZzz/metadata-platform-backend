package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.RecognitionRuleConvertor;
import com.yss.datasecurity.application.dto.RecognitionRuleBatchRunDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleCreateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleManualScanDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleTestResultVO;
import com.yss.datasecurity.application.dto.RecognitionRuleTransferOwnerDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleUpdateDTO;
import com.yss.datasecurity.application.dto.RecognitionRuleVO;
import com.yss.datasecurity.application.service.RecognitionRuleAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.RecognitionRuleGateway;
import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.domain.model.RecognitionTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecognitionRuleAppServiceImpl implements RecognitionRuleAppService {

    private final RecognitionRuleGateway recognitionRuleGateway;
    private final RecognitionRuleConvertor convertor;

    @Override
    public PageResult<RecognitionRuleVO> pageRules(int pageIndex, int pageSize, String keyword, Long categoryId, String owner, Boolean onlyMine, String currentUsername) {
        List<RecognitionRule> list = recognitionRuleGateway.pageRules(pageIndex, pageSize, keyword, categoryId, owner, onlyMine, currentUsername);
        long total = recognitionRuleGateway.countRules(keyword, categoryId, owner, onlyMine, currentUsername);
        List<RecognitionRuleVO> voList = convertor.toVOList(list);
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    public RecognitionRuleVO getDetail(Long id) {
        RecognitionRule rule = recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在: " + id));
        return convertor.toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RecognitionRuleCreateDTO dto) {
        recognitionRuleGateway.findByName(dto.getRuleName().trim()).ifPresent(r -> {
            throw new DataSecurityException("RULE_NAME_DUPLICATE", "识别规则名称已存在: " + dto.getRuleName());
        });

        RecognitionRule domain = convertor.toDomain(dto);
        if (domain.getOwner() == null || domain.getOwner().trim().isEmpty()) {
            domain.setOwner("admin");
        }
        domain.validate();
        RecognitionRule saved = recognitionRuleGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RecognitionRuleUpdateDTO dto) {
        RecognitionRule rule = recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在: " + id));

        recognitionRuleGateway.findByName(dto.getRuleName().trim()).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new DataSecurityException("RULE_NAME_DUPLICATE", "识别规则名称已存在: " + dto.getRuleName());
            }
        });

        convertor.updateDomainFromDTO(dto, rule);
        rule.validate();
        recognitionRuleGateway.update(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在: " + id));
        recognitionRuleGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在: " + id));
        recognitionRuleGateway.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetRule(Long id) {
        recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在: " + id));
        recognitionRuleGateway.clearTaggedFields(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cloneRule(Long id) {
        RecognitionRule source = recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("RECOGNITION_RULE_NOT_FOUND", "待克隆识别规则不存在: " + id));

        String cloneName = source.getRuleName() + "_copy";
        if (cloneName.length() > 12) {
            cloneName = cloneName.substring(0, 12);
        }

        // 避免重名
        int counter = 1;
        String finalName = cloneName;
        while (recognitionRuleGateway.findByName(finalName).isPresent()) {
            finalName = (cloneName.length() > 10 ? cloneName.substring(0, 10) : cloneName) + counter++;
        }

        RecognitionRule clone = RecognitionRule.builder()
                .ruleName(finalName)
                .description("克隆自: " + source.getRuleName() + (source.getDescription() != null ? " - " + source.getDescription() : ""))
                .categoryScopeMode(source.getCategoryScopeMode())
                .categoryScopeConfig(source.getCategoryScopeConfig())
                .scanSourceType(source.getScanSourceType())
                .computeScopeConfig(source.getComputeScopeConfig())
                .datasourceScopeConfig(source.getDatasourceScopeConfig())
                .owner(source.getOwner())
                .status("ENABLED")
                .priority(source.getPriority())
                .taggedFieldsCount(0)
                .lineageInheritanceEnabled(source.getLineageInheritanceEnabled())
                .build();

        RecognitionRule saved = recognitionRuleGateway.save(clone);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(RecognitionRuleTransferOwnerDTO dto) {
        if (dto.getRuleIds() == null || dto.getRuleIds().isEmpty()) {
            return;
        }
        for (Long id : dto.getRuleIds()) {
            recognitionRuleGateway.updateOwner(id, dto.getNewOwner());
        }
    }

    @Override
    public int batchRun(RecognitionRuleBatchRunDTO dto) {
        if (dto.getRuleIds() != null && !dto.getRuleIds().isEmpty()) {
            return dto.getRuleIds().size();
        }
        List<RecognitionRule> activeRules = recognitionRuleGateway.listAllActiveRules();
        return activeRules.size();
    }

    @Override
    public int manualScan(RecognitionRuleManualScanDTO dto) {
        List<RecognitionRule> activeRules = recognitionRuleGateway.listAllActiveRules();
        return activeRules.size();
    }

    @Override
    public List<RecognitionRuleTestResultVO> testRule(RecognitionRuleTestDTO dto) {
        List<RecognitionTestResult> results = new ArrayList<>();
        List<String> targets = dto.getTargetIdentifiers() != null ? dto.getTargetIdentifiers() : java.util.Arrays.asList("demo_proj", "user_center");
        String ruleName = "测试规则";
        if (dto.getRuleId() != null) {
            ruleName = recognitionRuleGateway.findById(dto.getRuleId()).map(RecognitionRule::getRuleName).orElse("已选规则");
        } else if (dto.getRuleDraft() != null && dto.getRuleDraft().getRuleName() != null) {
            ruleName = dto.getRuleDraft().getRuleName();
        }

        for (int i = 0; i < targets.size(); i++) {
            String target = targets.get(i);
            results.add(RecognitionTestResult.builder()
                    .projectOrDatasource(target)
                    .tableName("t_user_profile_" + (i + 1))
                    .columnName("id_card_no")
                    .columnComment("居民身份证号码")
                    .dataType("varchar(32)")
                    .sampleValue("110101199003072345")
                    .matchedCategory("居民身份证")
                    .matchedGrade("L4 (极度敏感)")
                    .confidence(0.98)
                    .matchedRule(ruleName)
                    .build());

            results.add(RecognitionTestResult.builder()
                    .projectOrDatasource(target)
                    .tableName("t_user_profile_" + (i + 1))
                    .columnName("phone_number")
                    .columnComment("联系电话/手机号")
                    .dataType("varchar(20)")
                    .sampleValue("13812345678")
                    .matchedCategory("移动电话")
                    .matchedGrade("L3 (敏感数据)")
                    .confidence(0.95)
                    .matchedRule(ruleName)
                    .build());

            results.add(RecognitionTestResult.builder()
                    .projectOrDatasource(target)
                    .tableName("t_user_profile_" + (i + 1))
                    .columnName("bank_card_no")
                    .columnComment("结算银行卡号")
                    .dataType("varchar(32)")
                    .sampleValue("6222021234567890123")
                    .matchedCategory("银行卡号")
                    .matchedGrade("L4 (极度敏感)")
                    .confidence(0.92)
                    .matchedRule(ruleName)
                    .build());
        }

        return convertor.toTestVOList(results);
    }
}
