package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.datamiddle.semantic.application.model.SynonymSetCreateInput;
import com.yss.datamiddle.semantic.application.service.SynonymSetService;
import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertorImpl;
import com.yss.datamiddle.semantic.rest.exception.SemanticExceptionAdvice;
import com.yss.datamiddle.semantic.synonym.exception.SynonymConceptConflictException;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SynonymSetControllerContractTest {

    @Mock
    private SynonymSetService synonymSetService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SynonymSetController controller = new SynonymSetController(synonymSetService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SemanticExceptionAdvice(new TermWebConvertorImpl()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("CT-01: 创建同义词组成功")
    void createSynonymSetSuccess() throws Exception {
        SynonymSet s = SynonymSet.create("营收组", "营收", Arrays.asList("营收", "收入"), null, "u1");
        s.setId(1L);
        when(synonymSetService.create(any(SynonymSetCreateInput.class))).thenReturn(s);

        String json = "{\"name\":\"营收组\",\"canonical\":\"营收\",\"words\":[\"营收\",\"收入\"]}";

        mockMvc.perform(post("/api/semantic/synonym-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.canonical").value("营收"));
    }

    @Test
    @DisplayName("CT-08: 主词或组名冲突返回 409 SYNONYM_CONCEPT_CONFLICT")
    void conceptConflictThrows409() throws Exception {
        when(synonymSetService.create(any(SynonymSetCreateInput.class)))
                .thenThrow(new SynonymConceptConflictException("主词已存在: 营收"));

        String json = "{\"name\":\"营收组\",\"canonical\":\"营收\",\"words\":[\"营收\",\"收入\"]}";

        mockMvc.perform(post("/api/semantic/synonym-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }
}
