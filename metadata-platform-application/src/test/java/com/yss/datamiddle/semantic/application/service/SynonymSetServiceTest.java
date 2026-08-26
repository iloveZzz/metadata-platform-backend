package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.SynonymSetCreateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.synonym.exception.SynonymConceptConflictException;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SynonymSetServiceTest {

    private final SynonymSetGateway gateway = Mockito.mock(SynonymSetGateway.class);
    private final CurrentUserPort userPort = new CurrentUserPort() {
        @Override
        public String userName() {
            return "user1";
        }

        @Override
        public boolean isWritePermitted() {
            return true;
        }
    };
    private final SynonymSetService service = new SynonymSetService(gateway, userPort);

    @Test
    @DisplayName("新建同义词组主词冲突抛出 409 SYNONYM_CONCEPT_CONFLICT")
    void conflictCanonicalThrows409() {
        when(gateway.findByCanonical("营收")).thenReturn(Optional.of(new SynonymSet()));

        SynonymSetCreateInput input = SynonymSetCreateInput.builder()
                .name("营收组2")
                .canonical("营收")
                .words(Arrays.asList("营收", "收入"))
                .build();

        assertThrows(SynonymConceptConflictException.class, () -> service.create(input));
    }

    @Test
    @DisplayName("正常创建同义词组")
    void createSuccess() {
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SynonymSetCreateInput input = SynonymSetCreateInput.builder()
                .name("营收组")
                .canonical("营收")
                .words(Arrays.asList("营收", "收入"))
                .build();

        SynonymSet created = service.create(input);
        assertNotNull(created);
        assertEquals("营收组", created.getName());
    }
}
