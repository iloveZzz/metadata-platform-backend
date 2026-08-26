package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.datamiddle.semantic.application.model.TermCertifyInput;
import com.yss.datamiddle.semantic.application.model.TermCreateInput;
import com.yss.datamiddle.semantic.application.model.TermUpdateInput;
import com.yss.datamiddle.semantic.application.service.TermQueryService;
import com.yss.datamiddle.semantic.application.service.TermService;
import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertor;
import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertorImpl;
import com.yss.datamiddle.semantic.rest.exception.SemanticExceptionAdvice;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.ReferenceConflictException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import com.yss.datamiddle.semantic.term.exception.TermNameDuplicateException;
import com.yss.datamiddle.semantic.term.exception.VersionConflictException;
import com.yss.datamiddle.semantic.term.gateway.TermPage;
import com.yss.datamiddle.semantic.term.gateway.TermQuery;
import com.yss.datamiddle.semantic.term.model.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 契约测试（冻结契约 semantic-layer.yaml v0.1.0-frozen）：
 * CT-02（Result/PageResult 包装、0 条空分页）/ CT-03（422 TERM_NAME_DUPLICATE / REQUIRED）
 * / CT-04（409 VERSION_CONFLICT + 最新对象）/ CT-07（删除被引用 409）/ CT-09（certify/deprecate 幂等）
 * / CT-10（只读用户直调写接口 403 + 审计，本层验证 403 映射，审计由 TermServiceTest 验证）。
 */
@ExtendWith(MockitoExtension.class)
class TermControllerContractTest {

    @Mock
    private TermService termService;
    @Mock
    private TermQueryService termQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TermWebConvertor convertor = new TermWebConvertorImpl();
        TermController controller = new TermController(termService, termQueryService, convertor);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SemanticExceptionAdvice(convertor))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ---- CT-02：Result / PageResult 包装 + 0 条空分页 ----

    @Test
    void ct02_getTerms_emptyResult_shouldReturn200EmptyPageNotError() throws Exception {
        when(termQueryService.pageTerms(any(TermQuery.class))).thenReturn(TermPage.builder()
                .list(Collections.emptyList()).totalCount(0).pageIndex(1).pageSize(20).build());

        mockMvc.perform(get("/api/semantic/terms").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    void ct02_createTerm_shouldReturn201WithResultWrapper() throws Exception {
        Term term = Term.create("营收", Arrays.asList("收入"), "营业收入口径", "描述", "张治理", "alice");
        term.setId(1L);
        when(termService.createTerm(any(TermCreateInput.class))).thenReturn(term);

        mockMvc.perform(post("/api/semantic/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"营收\",\"aliases\":[\"收入\"],"
                                + "\"definition\":\"营业收入口径\",\"owner\":\"张治理\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("营收"))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.owner").value("张治理"));
    }

    // ---- CT-03：422 字段级错误（TERM_NAME_DUPLICATE / REQUIRED） ----

    @Test
    void ct03_createTerm_duplicateName_shouldReturn422FieldError() throws Exception {
        when(termService.createTerm(any(TermCreateInput.class)))
                .thenThrow(new TermNameDuplicateException("营收"));

        mockMvc.perform(post("/api/semantic/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"营收\",\"definition\":\"定义\",\"owner\":\"张治理\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TERM_NAME_DUPLICATE"));
    }

    @Test
    void ct03_createTerm_missingRequiredFields_shouldReturn422Required() throws Exception {
        mockMvc.perform(post("/api/semantic/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliases\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARAM_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].code", hasSize(3)))
                .andExpect(jsonPath("$.fieldErrors[*].code", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("REQUIRED"))));
    }

    @Test
    void ct03_getTerms_invalidStatus_shouldReturn422InvalidEnum() throws Exception {
        mockMvc.perform(get("/api/semantic/terms").param("status", "bogus"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_ENUM"));
    }

    // ---- CT-04：乐观锁 409 VERSION_CONFLICT + 最新对象 ----

    @Test
    void ct04_updateTerm_staleVersion_shouldReturn409WithLatestObject() throws Exception {
        Term latest = Term.create("营收", Arrays.asList("收入"), "定义", null, "张治理", "alice");
        latest.setId(1L);
        latest.setVersion(3);
        when(termService.updateTerm(eq(1L), any(TermUpdateInput.class)))
                .thenThrow(new VersionConflictException("版本过期，已被他人修改，请刷新后重试", latest));

        mockMvc.perform(put("/api/semantic/terms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"营收\",\"definition\":\"定义\","
                                + "\"owner\":\"张治理\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.data.name").value("营收"))
                .andExpect(jsonPath("$.data.version").value(3))
                .andExpect(jsonPath("$.data.status").value("draft"));
    }

    // ---- CT-07：删除被引用 / 非草稿 409 ----

    @Test
    void ct07_delete_nonDraftTerm_shouldReturn409StateConflict() throws Exception {
        org.mockito.Mockito.doThrow(
                new StateConflictException("仅草稿状态的术语可删除，请改用弃用"))
                .when(termService).deleteTerm(1L);

        mockMvc.perform(delete("/api/semantic/terms/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void ct07_delete_referencedDraft_shouldReturn409ReferenceConflict() throws Exception {
        org.mockito.Mockito.doThrow(
                new ReferenceConflictException("术语已被挂接或关联同义词组，不可删除，请改用弃用"))
                .when(termService).deleteTerm(1L);

        mockMvc.perform(delete("/api/semantic/terms/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFERENCE_CONFLICT"));
    }

    // ---- CT-09：certify / deprecate 幂等返回当前状态 ----

    @Test
    void ct09_certify_alreadyCertified_shouldReturn200CurrentState() throws Exception {
        Term certified = Term.create("营收", Arrays.asList("收入"), "定义", null, "张治理", "alice");
        certified.setId(1L);
        certified.certify("alice");
        when(termService.certifyTerm(eq(1L), any(TermCertifyInput.class))).thenReturn(certified);

        mockMvc.perform(post("/api/semantic/terms/1/certify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"certify\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("certified"));
    }

    @Test
    void ct09_deprecate_alreadyDeprecated_shouldReturn200CurrentState() throws Exception {
        Term deprecated = Term.create("毛利", Collections.emptyList(), "定义", null, "张治理", "alice");
        deprecated.setId(2L);
        deprecated.deprecate("alice");
        when(termService.certifyTerm(eq(2L), any(TermCertifyInput.class))).thenReturn(deprecated);

        mockMvc.perform(post("/api/semantic/terms/2/certify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"deprecate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deprecated"));
    }

    @Test
    void ct09_certify_missingAction_shouldReturn422Required() throws Exception {
        mockMvc.perform(post("/api/semantic/terms/1/certify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void ct09_certify_invalidAction_shouldReturn422InvalidEnum() throws Exception {
        mockMvc.perform(post("/api/semantic/terms/1/certify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"bogus\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("action"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_ENUM"));
    }

    // ---- CT-10：只读用户直调写接口 403（审计由 Application 层验证） ----

    @Test
    void ct10_readOnlyUser_directUpdate_shouldReturn403() throws Exception {
        when(termService.updateTerm(eq(1L), any(TermUpdateInput.class)))
                .thenThrow(new PermissionDeniedException("当前用户为只读角色，无写操作权限"));

        mockMvc.perform(put("/api/semantic/terms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"营收\",\"definition\":\"定义\","
                                + "\"owner\":\"张治理\",\"version\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void ct10_readOnlyUser_directDelete_shouldReturn403() throws Exception {
        org.mockito.Mockito.doThrow(
                new PermissionDeniedException("当前用户为只读角色，无写操作权限"))
                .when(termService).deleteTerm(1L);

        mockMvc.perform(delete("/api/semantic/terms/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }
}
