package com.yss.datamiddle.semantic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yss.datamiddle.semantic.application.model.AttachmentCreateInput;
import com.yss.datamiddle.semantic.application.service.AttachmentService;
import com.yss.datamiddle.semantic.attachment.exception.AttachmentExistsException;
import com.yss.datamiddle.semantic.attachment.model.Attachment;
import com.yss.datamiddle.semantic.attachment.model.AttachmentLevel;
import com.yss.datamiddle.semantic.attachment.model.SemanticObjectType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerContractTest {

    @Mock
    private AttachmentService attachmentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        AttachmentController controller = new AttachmentController(attachmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SemanticExceptionAdvice(new TermWebConvertorImpl()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("CT-01: 创建挂接成功")
    void createAttachmentSuccess() throws Exception {
        Attachment a = Attachment.create(10L, AttachmentLevel.TABLE, null, SemanticObjectType.TERM, 1L, "u1");
        a.setId(100L);
        when(attachmentService.create(any(AttachmentCreateInput.class))).thenReturn(a);

        String json = "{\"assetId\":10,\"level\":\"TABLE\",\"semanticType\":\"TERM\",\"semanticId\":1}";

        mockMvc.perform(post("/api/semantic/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("CT-07: 重复挂接返回 409 ATTACHMENT_EXISTS")
    void duplicateAttachmentThrows409() throws Exception {
        when(attachmentService.create(any(AttachmentCreateInput.class)))
                .thenThrow(new AttachmentExistsException(99L));

        String json = "{\"assetId\":10,\"level\":\"TABLE\",\"semanticType\":\"TERM\",\"semanticId\":1}";

        mockMvc.perform(post("/api/semantic/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }
}
