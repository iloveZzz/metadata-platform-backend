package com.yss.metadata.rest;

import com.yss.metadata.application.governance.service.ClassificationGovernanceService;
import com.yss.metadata.application.governance.service.convertor.GovernanceAppConvertor;
import com.yss.metadata.application.governance.service.impl.ClassificationGovernanceServiceImpl;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.governance.model.ClassRule;
import com.yss.metadata.domain.governance.model.ClassRuleType;
import com.yss.metadata.domain.governance.model.Classification;
import com.yss.metadata.domain.governance.model.ClassificationStatus;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageEdge;
import com.yss.metadata.domain.lineage.model.LineageType;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryClassRuleGateway;
import com.yss.metadata.rest.support.InMemoryClassificationGateway;
import com.yss.metadata.rest.support.InMemoryImpactAnalysisRepository;
import com.yss.metadata.rest.support.InMemoryPropagateTaskGateway;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分级分类 REST 契约测试（WU-04-05，冻结 OpenAPI classifications 段）。
 *
 * <p>覆盖：概览组合 VO（0 数据空结构）、规则创建（201 + 审计 + 422）、
 * 规则启停（200/404/422）、候选确认/修正（200 幂等/404）、
 * 传播（202 + coverage + 审计 + 幂等复用 + 404/422）。</p>
 */
class ClassificationControllerTest {

    private static final String CLASSIFICATIONS_PATH = "/api/classifications";

    private InMemoryClassRuleGateway classRuleGateway;
    private InMemoryClassificationGateway classificationGateway;
    private InMemoryPropagateTaskGateway propagateTaskGateway;
    private InMemoryImpactAnalysisRepository impactAnalysisRepository;
    private InMemoryAssetRepository assetRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        classRuleGateway = new InMemoryClassRuleGateway();
        classificationGateway = new InMemoryClassificationGateway();
        propagateTaskGateway = new InMemoryPropagateTaskGateway();
        impactAnalysisRepository = new InMemoryImpactAnalysisRepository();
        assetRepository = new InMemoryAssetRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        ClassificationGovernanceService service = new ClassificationGovernanceServiceImpl(
                classRuleGateway, classificationGateway, propagateTaskGateway,
                impactAnalysisRepository, assetRepository, auditLogRepository,
                org.mapstruct.factory.Mappers.getMapper(GovernanceAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new ClassificationController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    // ---------- GET /api/classifications ----------

    @Test
    @DisplayName("概览 200：0 数据返回空结构（rules/results 空数组，非错误）")
    void overviewEmptyReturns200() throws Exception {
        mockMvc.perform(get(CLASSIFICATIONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rules", hasSize(0)))
                .andExpect(jsonPath("$.data.results", hasSize(0)));
    }

    @Test
    @DisplayName("概览 200：规则 + 结果组合返回（含 assetName/columnName 展示字段）")
    void overviewReturns200WithData() throws Exception {
        classRuleGateway.seed(rule("r-1", "内置-手机号", ClassRuleType.BUILTIN, "phone", true));
        seedAsset("a-1", "orders", null);
        assetRepository.seedColumns("a-1", java.util.Collections.singletonList(
                com.yss.metadata.domain.asset.model.AssetColumn.builder()
                        .id("col-1").name("mobile_no").type("varchar").build()));
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        mockMvc.perform(get(CLASSIFICATIONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[0].id").value("r-1"))
                .andExpect(jsonPath("$.data.rules[0].type").value("builtin"))
                .andExpect(jsonPath("$.data.results[0].id").value("c-1"))
                .andExpect(jsonPath("$.data.results[0].status").value("pending"))
                .andExpect(jsonPath("$.data.results[0].assetName").value("orders"))
                .andExpect(jsonPath("$.data.results[0].columnName").value("mobile_no"));
    }

    // ---------- POST /api/classifications ----------

    @Test
    @DisplayName("创建规则 201：小写枚举 type=regex + 默认启用 + 审计 classify.rule")
    void createRuleReturns201() throws Exception {
        mockMvc.perform(post(CLASSIFICATIONS_PATH)
                        .header("X-User-Id", "u-me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"手机号正则\",\"type\":\"regex\","
                                + "\"pattern\":\"^1[3-9]\\\\d{9}$\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("手机号正则"))
                .andExpect(jsonPath("$.data.type").value("regex"))
                .andExpect(jsonPath("$.data.pattern", startsWith("^1[3-9]")))
                .andExpect(jsonPath("$.data.enabled").value(true));

        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("classify.rule");
        assertThat(auditLogRepository.entries().get(0).getOperator()).isEqualTo("u-me");
    }

    @Test
    @DisplayName("创建规则 422：缺必填字段（name/type/pattern fieldErrors）")
    void createRuleMissingFieldReturns422() throws Exception {
        mockMvc.perform(post(CLASSIFICATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"regex\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'pattern')]", hasSize(1)));

        mockMvc.perform(post(CLASSIFICATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"规则\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]", hasSize(1)));
    }

    @Test
    @DisplayName("创建规则 422：非法枚举 type 值（请求体解析失败）")
    void createRuleInvalidEnumReturns422() throws Exception {
        mockMvc.perform(post(CLASSIFICATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"规则\",\"type\":\"bogus\",\"pattern\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"));
    }

    // ---------- PUT /api/classifications/{id}/status ----------

    @Test
    @DisplayName("规则启停 200：翻转 enabled + 审计 classify.rule.status")
    void toggleRuleReturns200() throws Exception {
        classRuleGateway.seed(rule("r-1", "正则", ClassRuleType.REGEX, "^a.*", true));

        mockMvc.perform(put(CLASSIFICATIONS_PATH + "/r-1/status")
                        .header("X-User-Id", "u-me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("r-1"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("classify.rule.status");
        assertThat(auditLogRepository.entries().get(0).getResult()).isEqualTo("disabled");
    }

    @Test
    @DisplayName("规则启停 404：规则不存在（class_rule.not_found）")
    void toggleRuleNotFoundReturns404() throws Exception {
        mockMvc.perform(put(CLASSIFICATIONS_PATH + "/not-exist/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("class_rule.not_found"));
    }

    @Test
    @DisplayName("规则启停 422：缺 enabled")
    void toggleRuleMissingEnabledReturns422() throws Exception {
        classRuleGateway.seed(rule("r-1", "正则", ClassRuleType.REGEX, "^a.*", true));

        mockMvc.perform(put(CLASSIFICATIONS_PATH + "/r-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("param.invalid"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'enabled')]", hasSize(1)));
    }

    // ---------- POST /api/classifications/{id}/confirm ----------

    @Test
    @DisplayName("候选确认 200：pending → confirmed（重复确认幂等）")
    void confirmReturns200() throws Exception {
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    @DisplayName("候选修正 200：correctedName 覆盖分类名并流转已修正")
    void confirmWithCorrectedNameReturns200() throws Exception {
        classificationGateway.seed(pending("c-1", "a-1", "col-1", "敏感-PII", "PII"));

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctedName\":\"内部受限\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("内部受限"))
                .andExpect(jsonPath("$.data.status").value("corrected"));
    }

    @Test
    @DisplayName("候选确认 404：分类不存在（classification.not_found）")
    void confirmNotFoundReturns404() throws Exception {
        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/not-exist/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("classification.not_found"));
    }

    // ---------- POST /api/classifications/{id}/propagate ----------

    @Test
    @DisplayName("传播 202：coverage 可核验 + 审计 classify.propagate + 操作者上下文")
    void propagateReturns202() throws Exception {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedEdge("a-root", "a-1");
        seedAsset("a-1", "ads_order_1d", null);

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/propagate")
                        .header("X-User-Id", "u-me"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.classificationId").value("c-1"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.coverage").value("2"))
                .andExpect(jsonPath("$.data.operator").value("u-me"));

        assertThat(assetRepository.store().get("a-1").getClassification()).isEqualTo("敏感-PII");
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("classify.propagate");
        assertThat(auditLogRepository.entries().get(0).getObject()).isNotBlank();
    }

    @Test
    @DisplayName("传播幂等 202：同 classification+version 复用既有任务，不重复审计")
    void propagateIdempotentReusesTask() throws Exception {
        classificationGateway.seed(pending("c-1", "a-root", "col-1", "敏感-PII", "PII"));
        seedAsset("a-root", "dwd_order_di", null);
        seedEdge("a-root", "a-1");
        seedAsset("a-1", "ads_order_1d", null);

        String firstTaskId = JsonPath.read(mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/propagate")
                        .header("X-User-Id", "u-me"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/propagate")
                        .header("X-User-Id", "u-other"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value(firstTaskId));

        assertThat(auditLogRepository.entries()).hasSize(1);
    }

    @Test
    @DisplayName("传播 404：分类不存在（classification.not_found）")
    void propagateNotFoundReturns404() throws Exception {
        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/not-exist/propagate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("classification.not_found"));
    }

    @Test
    @DisplayName("传播 422：分类未关联资产（asset.param.invalid）")
    void propagateWithoutAssetReturns422() throws Exception {
        classificationGateway.seed(Classification.builder()
                .id("c-1").assetId(" ").columnId(null)
                .name("敏感-PII").level("PII").source("auto")
                .status(ClassificationStatus.PENDING).build());

        mockMvc.perform(post(CLASSIFICATIONS_PATH + "/c-1/propagate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"));
    }

    // ---------- 辅助 ----------

    private void seedEdge(String from, String to) {
        impactAnalysisRepository.seedEdge(LineageEdge.builder().id("e-" + from + "-" + to)
                .fromAssetId(from).toAssetId(to)
                .type(LineageType.SQL).confidence(LineageConfidence.AUTO_HIGH).build());
    }

    private void seedAsset(String id, String name, String classification) {
        assetRepository.seed(Asset.builder().id(id).sourceId("s-1").name(name).type("table")
                .domain("交易域").owner(null).classification(classification).status(AssetStatus.CLAIMED)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build());
    }

    private ClassRule rule(String id, String name, ClassRuleType type, String pattern, boolean enabled) {
        return ClassRule.builder().id(id).name(name).type(type).pattern(pattern).enabled(enabled).build();
    }

    private Classification pending(String id, String assetId, String columnId, String name, String level) {
        return Classification.builder().id(id).assetId(assetId).columnId(columnId)
                .name(name).level(level).source("auto").status(ClassificationStatus.PENDING).build();
    }
}
