package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymRecommendation;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.model.Term;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SynonymRecommendationServiceTest {

    private SynonymSetGateway synonymSetGateway;
    private TermGateway termGateway;
    private CurrentUserPort currentUserPort;
    private SynonymRecommendationService service;

    @BeforeEach
    public void setUp() {
        synonymSetGateway = Mockito.mock(SynonymSetGateway.class);
        termGateway = Mockito.mock(TermGateway.class);
        currentUserPort = Mockito.mock(CurrentUserPort.class);
        Mockito.when(currentUserPort.isWritePermitted()).thenReturn(true);
        Mockito.when(currentUserPort.userName()).thenReturn("test-user");

        service = new SynonymRecommendationService(synonymSetGateway, termGateway, currentUserPort);
    }

    @Test
    public void testRecommendSynonymsFromTermsAndSets() {
        Term t1 = new Term();
        t1.setId(1L);
        t1.setName("营业收入");
        t1.setAliases(Arrays.asList("营收总额"));
        Mockito.when(termGateway.listAll()).thenReturn(Collections.singletonList(t1));

        SynonymSet s1 = SynonymSet.create("主营收入", "主营收入", Arrays.asList("主营收入", "主营业务收入"), null, "admin");
        s1.setId(10L);
        Mockito.when(synonymSetGateway.listAll()).thenReturn(Collections.singletonList(s1));

        List<SynonymRecommendation> list = service.recommendSynonyms("营收", 5);

        Assertions.assertFalse(list.isEmpty());
        Assertions.assertTrue(list.stream().anyMatch(r -> r.getCandidateWord().equals("营业收入")));
    }

    @Test
    public void testAcceptRecommendation() {
        SynonymSet s1 = SynonymSet.create("主营收入", "主营收入", Arrays.asList("主营收入"), null, "admin");
        s1.setId(10L);
        Mockito.when(synonymSetGateway.findById(10L)).thenReturn(java.util.Optional.of(s1));

        service.acceptRecommendation(10L, "主营业务收入");

        Mockito.verify(synonymSetGateway, Mockito.times(1)).update(Mockito.any(SynonymSet.class));
    }
}
