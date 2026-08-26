package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.semantic.application.service.SynonymRecommendationService;
import com.yss.datamiddle.semantic.client.dto.cmd.SynonymAcceptCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.SynonymRecommendationCmd;
import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SynonymRecommendationControllerContractTest {

    private MockMvc mockMvc;
    private SynonymRecommendationService recommendationService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        recommendationService = Mockito.mock(SynonymRecommendationService.class);
        SynonymRecommendationController controller = new SynonymRecommendationController(recommendationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testRecommendSynonymsContract() throws Exception {
        SynonymRecommendation rec = SynonymRecommendation.builder()
                .candidateWord("营业收入")
                .similarityScore(0.85)
                .matchReason("SUBSTRING_CONTAIN")
                .build();

        Mockito.when(recommendationService.recommendSynonyms("营收", 5))
                .thenReturn(Collections.singletonList(rec));

        SynonymRecommendationCmd cmd = SynonymRecommendationCmd.builder()
                .targetWord("营收")
                .limit(5)
                .build();

        mockMvc.perform(post("/api/semantic/synonyms/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data[0].candidateWord").value("营业收入"))
                .andExpect(jsonPath("$.data[0].similarityScore").value(0.85));
    }

    @Test
    public void testAcceptRecommendationContract() throws Exception {
        SynonymAcceptCmd cmd = SynonymAcceptCmd.builder()
                .synonymSetId(10L)
                .candidateWord("营业收入")
                .build();

        mockMvc.perform(post("/api/semantic/synonyms/recommend/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isNoContent());

        Mockito.verify(recommendationService, Mockito.times(1))
                .acceptRecommendation(10L, "营业收入");
    }
}
