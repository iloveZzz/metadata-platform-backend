package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.semantic.application.model.QueryExpansionResult;
import com.yss.datamiddle.semantic.application.model.SynonymExpansionItem;
import com.yss.datamiddle.semantic.application.service.SearchExpansionService;
import com.yss.datamiddle.semantic.rest.convertor.TermWebConvertorImpl;
import com.yss.datamiddle.semantic.rest.exception.SemanticExceptionAdvice;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchExpansionControllerContractTest {

    @Mock
    private SearchExpansionService searchExpansionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SearchExpansionController controller = new SearchExpansionController(searchExpansionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SemanticExceptionAdvice(new TermWebConvertorImpl()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("CT-12: POST /api/semantic/search/expand 词级同义展开成功")
    void expandSuccess() throws Exception {
        SynonymExpansionItem item = SynonymExpansionItem.builder()
                .synonymSetId(1L)
                .name("营收组")
                .canonical("营收")
                .words(Arrays.asList("营收", "营业收入"))
                .build();
        QueryExpansionResult result = QueryExpansionResult.builder()
                .query("营收")
                .expansions(Collections.singletonList(item))
                .build();

        when(searchExpansionService.expand(any())).thenReturn(Collections.singletonList(result));

        String json = "{\"queries\":[\"营收\"]}";

        mockMvc.perform(post("/api/semantic/search/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data[0].query").value("营收"))
                .andExpect(jsonPath("$.data[0].expansions[0].canonical").value("营收"));
    }
}
