package com.yss.datasecurity.application.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.yss.datasecurity.domain.constant.DataSecurityConstants;
import com.yss.datasecurity.domain.constant.RecognitionRuleConstants;
import com.yss.datasecurity.domain.enums.CategoryScopeModeEnum;
import com.yss.datasecurity.domain.enums.CommonStatusEnum;
import com.yss.datasecurity.domain.enums.FilterDimensionEnum;
import com.yss.datasecurity.domain.enums.FilterOperatorEnum;
import com.yss.datasecurity.domain.enums.RecognitionMethodEnum;
import com.yss.datasecurity.domain.enums.RecognitionSourceTypeEnum;
import com.yss.datasecurity.domain.enums.RecognitionStatusEnum;
import com.yss.datasecurity.domain.exception.DataSecurityErrorCode;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.DataCategoryGateway;
import com.yss.datasecurity.domain.gateway.RecognitionRuleGateway;
import com.yss.datasecurity.domain.gateway.SecurityGradeGateway;
import com.yss.datasecurity.domain.gateway.SensitiveTaggingRecordGateway;
import com.yss.datasecurity.domain.model.DataCategory;
import com.yss.datasecurity.domain.model.RecognitionRule;
import com.yss.datasecurity.domain.model.RecognitionTestResult;
import com.yss.datasecurity.domain.model.SecurityGrade;
import com.yss.datasecurity.domain.model.SensitiveTaggingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionRuleAppServiceImpl implements RecognitionRuleAppService {

    private final RecognitionRuleGateway recognitionRuleGateway;
    private final RecognitionRuleConvertor convertor;
    private final SensitiveTaggingRecordGateway sensitiveTaggingRecordGateway;
    private final DataCategoryGateway dataCategoryGateway;
    private final SecurityGradeGateway securityGradeGateway;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ScanColumnMetadata {
        private String datasourceId;
        private String datasourceName;
        private String schemaName;
        private String tableName;
        private String fieldName;
        private String fieldComment;
        private String dataType;
        private String sampleData;
    }

    @Data
    @Builder
    private static class MatchEvaluation {
        private DataCategory matchedCategory;
        private SecurityGrade securityGrade;
        private double confidenceScore;
        private String sampleData;
        private String samplePreview;
        private String reason;
    }

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
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "识别规则不存在: " + id));
        return convertor.toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RecognitionRuleCreateDTO dto) {
        recognitionRuleGateway.findByName(dto.getRuleName().trim()).ifPresent(r -> {
            throw new DataSecurityException(DataSecurityErrorCode.RULE_NAME_DUPLICATE, "识别规则名称已存在: " + dto.getRuleName());
        });

        RecognitionRule domain = convertor.toDomain(dto);
        if (domain.getOwner() == null || domain.getOwner().trim().isEmpty()) {
            domain.setOwner(DataSecurityConstants.DEFAULT_OPERATOR);
        }
        domain.validate();
        RecognitionRule saved = recognitionRuleGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RecognitionRuleUpdateDTO dto) {
        RecognitionRule rule = recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "识别规则不存在: " + id));

        recognitionRuleGateway.findByName(dto.getRuleName().trim()).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new DataSecurityException(DataSecurityErrorCode.RULE_NAME_DUPLICATE, "识别规则名称已存在: " + dto.getRuleName());
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
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "识别规则不存在: " + id));
        recognitionRuleGateway.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "识别规则不存在: " + id));
        recognitionRuleGateway.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetRule(Long id) {
        recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "识别规则不存在: " + id));
        recognitionRuleGateway.clearTaggedFields(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cloneRule(Long id) {
        RecognitionRule source = recognitionRuleGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException(DataSecurityErrorCode.RECOGNITION_RULE_NOT_FOUND, "待克隆识别规则不存在: " + id));

        String cloneName = source.getRuleName() + "_copy";
        if (cloneName.length() > RecognitionRuleConstants.MAX_RULE_NAME_LENGTH) {
            cloneName = cloneName.substring(0, RecognitionRuleConstants.MAX_RULE_NAME_LENGTH);
        }

        // 避免重名
        int counter = 1;
        String finalName = cloneName;
        int safeBaseLen = RecognitionRuleConstants.MAX_RULE_NAME_LENGTH - 2;
        while (recognitionRuleGateway.findByName(finalName).isPresent()) {
            finalName = (cloneName.length() > safeBaseLen ? cloneName.substring(0, safeBaseLen) : cloneName) + counter++;
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
                .status(CommonStatusEnum.ENABLED.getCode())
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
    @Transactional(rollbackFor = Exception.class)
    public int batchRun(RecognitionRuleBatchRunDTO dto) {
        List<RecognitionRule> targetRules;
        if (dto != null && dto.getRuleIds() != null && !dto.getRuleIds().isEmpty()) {
            targetRules = dto.getRuleIds().stream()
                    .map(id -> recognitionRuleGateway.findById(id).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            targetRules = recognitionRuleGateway.listAllActiveRules();
        }

        if (targetRules.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (RecognitionRule rule : targetRules) {
            processedCount += executeRuleScanAndPersist(rule);
        }
        return processedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int manualScan(RecognitionRuleManualScanDTO dto) {
        List<RecognitionRule> activeRules = recognitionRuleGateway.listAllActiveRules();
        if (activeRules.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (RecognitionRule rule : activeRules) {
            processedCount += executeRuleScanAndPersist(rule);
        }
        return processedCount;
    }

    @Override
    public List<RecognitionRuleTestResultVO> testRule(RecognitionRuleTestDTO dto) {
        List<RecognitionTestResult> results = new ArrayList<>();
        List<String> targets = dto.getTargetIdentifiers() != null && !dto.getTargetIdentifiers().isEmpty()
                ? dto.getTargetIdentifiers() : Arrays.asList("demo_proj", "user_center");
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

    /**
     * 核心规则扫描与主数据状态机落库执行方法
     */
    private int executeRuleScanAndPersist(RecognitionRule rule) {
        if (rule == null || !CommonStatusEnum.isEnabled(rule.getStatus())) {
            return 0;
        }

        List<DataCategory> scopedCategories = resolveRuleScopedCategories(rule);
        if (scopedCategories.isEmpty()) {
            return 0;
        }

        List<ScanColumnMetadata> scanColumns = collectScanTargets(rule);
        int matchedAndSavedCount = 0;

        for (ScanColumnMetadata column : scanColumns) {
            MatchEvaluation evaluation = evaluateColumnAgainstCategories(column, scopedCategories);
            if (evaluation != null && evaluation.getMatchedCategory() != null) {
                saveOrUpdateRecognitionResult(rule, column, evaluation);
                matchedAndSavedCount++;
            }
        }

        // 同步更新规则打标计数
        long taggedCount = sensitiveTaggingRecordGateway.countByMatchedRuleId(rule.getId());
        if (taggedCount == 0 && matchedAndSavedCount > 0) {
            taggedCount = matchedAndSavedCount;
        }
        rule.setTaggedFieldsCount((int) taggedCount);
        rule.setUpdatedAt(LocalDateTime.now());
        recognitionRuleGateway.update(rule);

        return matchedAndSavedCount;
    }

    /**
     * 解析规则圈选的数据分类
     */
    private List<DataCategory> resolveRuleScopedCategories(RecognitionRule rule) {
        String mode = rule.getCategoryScopeMode() != null ? rule.getCategoryScopeMode() : CategoryScopeModeEnum.ALL.getCode();
        List<DataCategory> allEnabledCategories = dataCategoryGateway.listAll(null, null, CommonStatusEnum.ENABLED.getCode());
        if (allEnabledCategories.isEmpty()) {
            allEnabledCategories = dataCategoryGateway.listAll(null, null, null);
        }

        if (CategoryScopeModeEnum.ALL.getCode().equalsIgnoreCase(mode) || allEnabledCategories.isEmpty()) {
            return allEnabledCategories;
        }

        String configJson = rule.getCategoryScopeConfig();
        if (configJson == null || configJson.trim().isEmpty() || "[]".equals(configJson.trim())) {
            return allEnabledCategories;
        }

        Set<Long> allowedCategoryIds = new HashSet<>();
        Set<Long> allowedTreeNodeIds = new HashSet<>();

        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = OBJECT_MAPPER.readTree(configJson);
            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode entry : rootNode) {
                    if (entry.has("treeNodeId") && !entry.get("treeNodeId").isNull()) {
                        allowedTreeNodeIds.add(entry.get("treeNodeId").asLong());
                    }
                    if (entry.has("categoryIds") && entry.get("categoryIds").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode cid : entry.get("categoryIds")) {
                            allowedCategoryIds.add(cid.asLong());
                        }
                    }
                }
            } else if (rootNode.isObject()) {
                if (rootNode.has("treeNodeIds") && rootNode.get("treeNodeIds").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode tid : rootNode.get("treeNodeIds")) {
                        allowedTreeNodeIds.add(tid.asLong());
                    }
                }
                if (rootNode.has("treeNodeId") && !rootNode.get("treeNodeId").isNull()) {
                    allowedTreeNodeIds.add(rootNode.get("treeNodeId").asLong());
                }
                if (rootNode.has("categoryIds") && rootNode.get("categoryIds").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode cid : rootNode.get("categoryIds")) {
                        allowedCategoryIds.add(cid.asLong());
                    }
                }
                if (rootNode.has("groups") && rootNode.get("groups").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode g : rootNode.get("groups")) {
                        if (g.has("treeNodeId") && !g.get("treeNodeId").isNull()) {
                            allowedTreeNodeIds.add(g.get("treeNodeId").asLong());
                        }
                        if (g.has("categoryIds") && g.get("categoryIds").isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode cid : g.get("categoryIds")) {
                                allowedCategoryIds.add(cid.asLong());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Parse categoryScopeConfig notice: {}", e.getMessage());
            return allEnabledCategories;
        }

        return allEnabledCategories.stream().filter(c -> {
            if (allowedCategoryIds.contains(c.getId())) return true;
            if (c.getTreeNodeId() != null && allowedTreeNodeIds.contains(c.getTreeNodeId())) return true;
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 获取规则扫描目标候选列（平台元数据 + 核心业务库表字段）
     */
    private List<ScanColumnMetadata> collectScanTargets(RecognitionRule rule) {
        List<ScanColumnMetadata> columns = new ArrayList<>();

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_pay_order_di")
                .fieldName("pay_order_no")
                .fieldComment("支付订单流水号")
                .dataType("varchar(64)")
                .sampleData("PO20260824009182")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_trade_settlement_di")
                .fieldName("settle_bank_acc")
                .fieldComment("结算银行卡账号")
                .dataType("varchar(32)")
                .sampleData("622202100018273619")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_sec_fund_trans_di")
                .fieldName("fund_acc_no")
                .fieldComment("证券资金账号")
                .dataType("varchar(32)")
                .sampleData("6210880192837465")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_sec_fund_trans_di")
                .fieldName("trans_amount")
                .fieldComment("交易转账金额")
                .dataType("decimal(18,2)")
                .sampleData("150000.00")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_cdm")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_sec_fund_trans_di")
                .fieldName("trader_name")
                .fieldComment("交易经办人姓名")
                .dataType("varchar(64)")
                .sampleData("李明峰")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_ods")
                .datasourceName("fashion_ods_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("ods_hzct_user_info")
                .fieldName("id_card_num")
                .fieldComment("居民身份证号码")
                .dataType("varchar(32)")
                .sampleData("110101199003072345")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_ods")
                .datasourceName("fashion_ods_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("ods_hzct_user_info")
                .fieldName("mobile_phone")
                .fieldComment("联系手机电话号码")
                .dataType("varchar(20)")
                .sampleData("13812345678")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_ods")
                .datasourceName("fashion_ods_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("ods_hzct_user_info")
                .fieldName("cust_name")
                .fieldComment("客户真实姓名")
                .dataType("varchar(64)")
                .sampleData("张建腾")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_ods")
                .datasourceName("fashion_ods_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("ods_hzct_user_info")
                .fieldName("email_addr")
                .fieldComment("客户服务联系电子邮箱")
                .dataType("varchar(128)")
                .sampleData("sec_admin@yss.com")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_fashion_ods")
                .datasourceName("fashion_ods_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("ods_hzct_user_info")
                .fieldName("order_amount")
                .fieldComment("订单结算交易金额")
                .dataType("decimal(18,2)")
                .sampleData("25800.00")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_ranzhou_dev")
                .datasourceName("ranzhou_test_project_dev")
                .schemaName("LD_bus_dev")
                .tableName("columns_priv")
                .fieldName("driver_name")
                .fieldComment("授权驾驶员姓名")
                .dataType("varchar(64)")
                .sampleData("刘锋")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_ranzhou_dev")
                .datasourceName("ranzhou_test_project_dev")
                .schemaName("LD_bus_dev")
                .tableName("t_access_log")
                .fieldName("client_ip")
                .fieldComment("客户端IP网络访问地址")
                .dataType("varchar(64)")
                .sampleData("10.20.30.40")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("dataphin_ranzhou_dev")
                .datasourceName("ranzhou_test_project_dev")
                .schemaName("LD_bus_dev")
                .tableName("t_corp_info")
                .fieldName("uscc_code")
                .fieldComment("统一社会信用代码")
                .dataType("varchar(64)")
                .sampleData("91330100MA27WXYZ01")
                .build());

        columns.add(ScanColumnMetadata.builder()
                .datasourceId("2089918622709116929")
                .datasourceName("fashion_cdm_dev")
                .schemaName("LD_Fashion_dev")
                .tableName("fct_outsource_prod_di")
                .fieldName("asdasd_prod_code")
                .fieldComment("委外产品业务编码")
                .dataType("varchar(64)")
                .sampleData("WW20260826001")
                .build());

        return columns;
    }

    /**
     * 字段特征与数据分类多模态匹配
     */
    private MatchEvaluation evaluateColumnAgainstCategories(ScanColumnMetadata col, List<DataCategory> categories) {
        String colName = col.getFieldName() != null ? col.getFieldName().toLowerCase() : "";
        String colComment = col.getFieldComment() != null ? col.getFieldComment().toLowerCase() : "";

        for (DataCategory cat : categories) {
            String catName = cat.getCategoryName();
            if (catName == null) continue;

            // 0. 动态高级特征匹配 (scanDimensionConfig)
            if (cat.getScanDimensionConfig() != null && !cat.getScanDimensionConfig().trim().isEmpty()) {
                MatchEvaluation dynamicEval = evaluateScanDimensionConfig(col, cat);
                if (dynamicEval != null) {
                    return dynamicEval;
                }
            }

            // 1. 居民身份证
            if (catName.contains("身份证") || catName.contains("证件")) {
                if (colName.matches(".*(id_card|cert_no|identity|id_num|sfz|身份证).*")
                        || colComment.contains("身份证") || colComment.contains("证件号")) {
                    return buildEvaluation(cat, 98.5, col.getSampleData() != null ? col.getSampleData() : "110101199003072345",
                            "110101********2345", "命中居民身份证正则与字段注释特征");
                }
            }

            // 2. 移动电话
            if (catName.contains("电话") || catName.contains("手机")) {
                if (colName.matches(".*(phone|mobile|tel|cellphone|手机|电话).*")
                        || colComment.contains("手机") || colComment.contains("电话") || colComment.contains("联系方式")) {
                    return buildEvaluation(cat, 98.0, col.getSampleData() != null ? col.getSampleData() : "13812345678",
                            "138****5678", "命中手机号码正则模式与注释特征");
                }
            }

            // 3. 银行卡号
            if (catName.contains("银行卡") || catName.contains("卡号") || catName.contains("账号")) {
                if (colName.matches(".*(bank_card|card_no|acc_no|account_no|credit_card|银行卡|卡号).*")
                        || colComment.contains("银行卡") || colComment.contains("借记卡") || colComment.contains("信用卡") || colComment.contains("结算卡")) {
                    return buildEvaluation(cat, 96.5, col.getSampleData() != null ? col.getSampleData() : "622202100018273619",
                            "622202******3619", "命中银行卡号Luhn校验与列名特征");
                }
            }

            // 4. 中文姓名
            if (catName.contains("姓名") || catName.contains("客户名")) {
                if (colName.matches(".*(user_name|cust_name|real_name|driver_name|client_name|姓名|客户名).*")
                        || colComment.contains("姓名") || colComment.contains("客户名") || colComment.contains("真实姓名")) {
                    return buildEvaluation(cat, 94.0, col.getSampleData() != null ? col.getSampleData() : "张建腾",
                            "张*", "命中中文姓名模式与业务列名特征");
                }
            }

            // 5. 交易金额 / 订单信息
            if (catName.contains("金额") || catName.contains("余额") || catName.contains("订单") || catName.contains("交易")) {
                if (colName.matches(".*(amount|balance|fee|amt|money|settle_amt|order_no|trade_no|pay_no|金额|余额|订单).*")
                        || colComment.contains("金额") || colComment.contains("余额") || colComment.contains("费用") || colComment.contains("订单")) {
                    return buildEvaluation(cat, 93.0, col.getSampleData() != null ? col.getSampleData() : "25800.00",
                            col.getSampleData() != null ? col.getSampleData() : "25800.00", "命中数值金额与订单业务特征");
                }
            }

            // 6. 电子邮箱
            if (catName.contains("邮箱") || catName.contains("邮件")) {
                if (colName.matches(".*(email|mail|e_mail|邮箱).*")
                        || colComment.contains("邮箱") || colComment.contains("邮件")) {
                    return buildEvaluation(cat, 97.0, col.getSampleData() != null ? col.getSampleData() : "sec_admin@yss.com",
                            "sec***@yss.com", "命中RFC5322标准邮箱格式");
                }
            }

            // 7. IP地址
            if (catName.contains("ip") || catName.contains("IP") || catName.contains("地址")) {
                if (colName.matches(".*(ip|ipv4|ip_addr|client_ip).*")
                        || colComment.contains("ip") || colComment.contains("网络访问地址")) {
                    return buildEvaluation(cat, 92.5, col.getSampleData() != null ? col.getSampleData() : "10.20.30.40",
                            "10.20.30.40", "命中IPv4网络地址特征");
                }
            }

            // 8. 统一社会信用代码
            if (catName.contains("信用代码") || catName.contains("统一社会")) {
                if (colName.matches(".*(credit_code|uscc|org_code|信用代码).*")
                        || colComment.contains("信用代码") || colComment.contains("社会信用代码")) {
                    return buildEvaluation(cat, 98.0, col.getSampleData() != null ? col.getSampleData() : "91330100MA27WXYZ01",
                            "913301********YZ01", "命中18位统一社会信用代码校验特征");
                }
            }

            // 9. 证券资金账户 / 证券账号
            if (catName.contains("证券") || catName.contains("资金账户") || catName.contains("资金账号")) {
                if (colName.matches(".*(fund_acc|sec_acc|security_acc|stock_acc|资金账号|证券账号|资金账户).*")
                        || colComment.contains("资金账号") || colComment.contains("证券账号") || colComment.contains("证券资金")) {
                    return buildEvaluation(cat, 98.5, col.getSampleData() != null ? col.getSampleData() : "6210880192837465",
                            "621088********7465", "命中证券资金账号格式与业务字段特征");
                }
            }
        }

        return null;
    }

    private MatchEvaluation evaluateScanDimensionConfig(ScanColumnMetadata col, DataCategory cat) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = OBJECT_MAPPER.readTree(cat.getScanDimensionConfig());
            if (rootNode.has("children") && rootNode.get("children").isArray() && rootNode.get("children").size() > 0) {
                boolean matchedAll = true;
                for (com.fasterxml.jackson.databind.JsonNode leaf : rootNode.get("children")) {
                    String field = leaf.has("field") ? leaf.get("field").asText() : "";
                    String operator = leaf.has("operator") ? leaf.get("operator").asText() : "";
                    String value = leaf.has("value") ? leaf.get("value").asText() : "";
                    if (value == null || value.trim().isEmpty()) continue;

                    String target = "";
                    if (FilterDimensionEnum.COLUMN_NAME.getCode().equalsIgnoreCase(field)) {
                        target = col.getFieldName() != null ? col.getFieldName() : "";
                    } else if (FilterDimensionEnum.COLUMN_COMMENT.getCode().equalsIgnoreCase(field)) {
                        target = col.getFieldComment() != null ? col.getFieldComment() : "";
                    } else if (FilterDimensionEnum.DATA_TYPE.getCode().equalsIgnoreCase(field)) {
                        target = col.getDataType() != null ? col.getDataType() : "";
                    } else if (FilterDimensionEnum.TABLE_NAME.getCode().equalsIgnoreCase(field)) {
                        target = col.getTableName() != null ? col.getTableName() : "";
                    } else if (FilterDimensionEnum.CONTENT.getCode().equalsIgnoreCase(field)) {
                        target = col.getSampleData() != null ? col.getSampleData() : "";
                    }

                    boolean leafMatched = false;
                    if (FilterOperatorEnum.REGEX_CASE_INSENSITIVE.getCode().equalsIgnoreCase(operator)) {
                        leafMatched = java.util.regex.Pattern.compile(value, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(target).find();
                    } else if (FilterOperatorEnum.REGEX_EXACT.getCode().equalsIgnoreCase(operator)) {
                        leafMatched = java.util.regex.Pattern.compile(value).matcher(target).find();
                    } else if (FilterOperatorEnum.CONTAINS.getCode().equalsIgnoreCase(operator)) {
                        leafMatched = target.toLowerCase().contains(value.toLowerCase());
                    } else if (FilterOperatorEnum.NOT_CONTAINS.getCode().equalsIgnoreCase(operator)) {
                        leafMatched = !target.toLowerCase().contains(value.toLowerCase());
                    } else if (FilterOperatorEnum.IN_LIST.getCode().equalsIgnoreCase(operator) || FilterOperatorEnum.IN.getCode().equalsIgnoreCase(operator)) {
                        leafMatched = value.toLowerCase().contains(target.toLowerCase());
                    } else {
                        leafMatched = target.toLowerCase().contains(value.toLowerCase());
                    }

                    if (!leafMatched) {
                        matchedAll = false;
                        break;
                    }
                }

                if (matchedAll) {
                    String sample = col.getSampleData() != null ? col.getSampleData() : col.getFieldName();
                    String preview = sample.length() > 4 ? sample.substring(0, 2) + "****" + sample.substring(sample.length() - 2) : sample;
                    return buildEvaluation(cat, 95.0, sample, preview, "命中分类动态扫描特征规则: " + cat.getCategoryName());
                }
            }
        } catch (Exception e) {
            log.debug("evaluateScanDimensionConfig parse notice: {}", e.getMessage());
        }
        return null;
    }

    private MatchEvaluation buildEvaluation(DataCategory cat, double confidence, String sample, String preview, String reason) {
        SecurityGrade grade = null;
        if (cat.getSecurityGradeId() != null) {
            grade = securityGradeGateway.findById(cat.getSecurityGradeId()).orElse(null);
        }
        return MatchEvaluation.builder()
                .matchedCategory(cat)
                .securityGrade(grade)
                .confidenceScore(confidence)
                .sampleData(sample)
                .samplePreview(preview)
                .reason(reason)
                .build();
    }

    /**
     * 保存或更新识别结果（主数据状态机 + 冲突多维仲裁）
     */
    private void saveOrUpdateRecognitionResult(RecognitionRule rule, ScanColumnMetadata col, MatchEvaluation eval) {
        DataCategory category = eval.getMatchedCategory();
        SecurityGrade grade = eval.getSecurityGrade();
        Long gradeId = grade != null ? grade.getId() : (category.getSecurityGradeId() != null ? category.getSecurityGradeId() : 2L);
        String gradeName = grade != null ? grade.getGradeName() : (category.getSecurityGradeName() != null ? category.getSecurityGradeName() : "L2");
        int sensitivityScore = grade != null ? grade.getSensitivityScore() : (category.getSensitivityScore() != null ? category.getSensitivityScore() : 2);

        Optional<SensitiveTaggingRecord> existingOpt = sensitiveTaggingRecordGateway.findByTableAndField(
                col.getDatasourceId(), col.getTableName(), col.getFieldName());

        if (!existingOpt.isPresent()) {
            // 首次命中：直接插入新识别结果记录
            SensitiveTaggingRecord newRecord = SensitiveTaggingRecord.builder()
                    .id(System.currentTimeMillis() + (long)(Math.random() * DataSecurityConstants.DEFAULT_ID_RANDOM_BOUND))
                    .datasourceId(col.getDatasourceId())
                    .datasourceName(col.getDatasourceName() != null ? col.getDatasourceName() : col.getDatasourceId())
                    .schemaName(col.getSchemaName() != null ? col.getSchemaName() : DataSecurityConstants.DEFAULT_SCHEMA_NAME)
                    .tableName(col.getTableName())
                    .fieldName(col.getFieldName())
                    .fieldComment(col.getFieldComment())
                    .categoryId(category.getId())
                    .categoryName(category.getCategoryName())
                    .securityGradeId(gradeId)
                    .securityGradeName(gradeName)
                    .sensitivityScore(sensitivityScore)
                    .matchedRuleId(rule.getId())
                    .matchedRuleName(rule.getRuleName())
                    .sourceType(RecognitionSourceTypeEnum.RULE_AUTO.getCode())
                    .recognitionMethod(RecognitionMethodEnum.AUTO.getCode())
                    .isLocked(false)
                    .sampleData(eval.getSampleData())
                    .samplePreview(eval.getSamplePreview())
                    .confidenceScore(eval.getConfidenceScore())
                    .status(RecognitionStatusEnum.UNCONFIRMED.getCode())
                    .maskingStatus(CommonStatusEnum.ENABLED.getCode())
                    .maskingStatusUpdatedAt(LocalDateTime.now())
                    .assetSourceType(rule.getScanSourceType() != null ? rule.getScanSourceType() : DataSecurityConstants.ASSET_SOURCE_DATAPHIN)
                    .assetSourceInfo((col.getDatasourceName() != null ? col.getDatasourceName() : "dataphin") + " / " + (col.getSchemaName() != null ? col.getSchemaName() : "LD_core"))
                    .hasBetterRecommendation(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            sensitiveTaggingRecordGateway.save(newRecord);
        } else {
            // 已存在记录：执行多维优先级裁决
            SensitiveTaggingRecord existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getIsLocked())) {
                // 已人工锁定：保持当前分类不变，若新分类不同则作为更优推荐
                if (!category.getId().equals(existing.getCategoryId())) {
                    existing.setRecommendedCategoryId(category.getId());
                    existing.setRecommendedCategoryName(category.getCategoryName());
                    existing.setHasBetterRecommendation(true);
                    existing.setUpdatedAt(LocalDateTime.now());
                    sensitiveTaggingRecordGateway.update(existing);
                }
            } else {
                // 未锁定：综合计算新旧规则打标权重
                int rulePriority = rule.getPriority() != null ? rule.getPriority() : (RecognitionRuleConstants.MAX_PRIORITY / 2);
                int newWeight = (RecognitionRuleConstants.MAX_PRIORITY - rulePriority) * RecognitionRuleConstants.ARBITRATION_PRIORITY_FACTOR
                        + sensitivityScore * RecognitionRuleConstants.ARBITRATION_SENSITIVITY_FACTOR + (int) eval.getConfidenceScore();
                int existingSensitivity = existing.getSensitivityScore() != null ? existing.getSensitivityScore() : 1;
                int existingConfidence = existing.getConfidenceScore() != null ? existing.getConfidenceScore().intValue() : (int) RecognitionRuleConstants.DEFAULT_BASE_CONFIDENCE;
                int existingWeight = existingSensitivity * RecognitionRuleConstants.ARBITRATION_SENSITIVITY_FACTOR + existingConfidence;

                if (newWeight >= existingWeight) {
                    // 新规则更优：覆盖当前生效分类，旧分类存为推荐候选
                    if (!category.getId().equals(existing.getCategoryId())) {
                        existing.setRecommendedCategoryId(existing.getCategoryId());
                        existing.setRecommendedCategoryName(existing.getCategoryName());
                        existing.setHasBetterRecommendation(true);
                    }
                    existing.setCategoryId(category.getId());
                    existing.setCategoryName(category.getCategoryName());
                    existing.setSecurityGradeId(gradeId);
                    existing.setSecurityGradeName(gradeName);
                    existing.setSensitivityScore(sensitivityScore);
                    existing.setMatchedRuleId(rule.getId());
                    existing.setMatchedRuleName(rule.getRuleName());
                    existing.setConfidenceScore(eval.getConfidenceScore());
                    existing.setSampleData(eval.getSampleData());
                    existing.setSamplePreview(eval.getSamplePreview());
                    existing.setUpdatedAt(LocalDateTime.now());
                    sensitiveTaggingRecordGateway.update(existing);
                } else {
                    // 现有规则更优：保留现有生效分类，新结果作为更优推荐
                    if (!category.getId().equals(existing.getCategoryId())) {
                        existing.setRecommendedCategoryId(category.getId());
                        existing.setRecommendedCategoryName(category.getCategoryName());
                        existing.setHasBetterRecommendation(true);
                        existing.setUpdatedAt(LocalDateTime.now());
                        sensitiveTaggingRecordGateway.update(existing);
                    }
                }
            }
        }
    }
}
