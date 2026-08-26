package com.yss.datasecurity.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.KeyPermissionDTO;
import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;
import com.yss.datasecurity.application.dto.KeyTransferDTO;
import com.yss.datasecurity.application.service.KeySecretAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.rest.advice.DataSecurityExceptionAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KeySecretController.class)
@ContextConfiguration(classes = {KeySecretController.class, DataSecurityExceptionAdvice.class})
class KeySecretControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeySecretAppService keySecretAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/keys - 分页查询密钥列表成功")
    void testPageKeys() throws Exception {
        KeySecretVO vo = KeySecretVO.builder()
            .id(7001L)
            .keyName("生产SM4密钥")
            .keyType("ENCRYPTION")
            .algorithm("SM4")
            .keyLength(128)
            .genType("SYSTEM")
            .owner("admin")
            .status("ACTIVE")
            .referencedRulesCount(2)
            .build();

        PageResult<KeySecretVO> pageResult = PageResult.of(Collections.singletonList(vo), 1, 20, 1);
        when(keySecretAppService.pageKeys(anyInt(), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/keys?pageIndex=1&pageSize=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].keyName").value("生产SM4密钥"))
            .andExpect(jsonPath("$.data[0].algorithm").value("SM4"));
    }

    @Test
    @DisplayName("POST /api/v1/keys - 注册密钥成功返回 201 Created")
    void testCreateKey() throws Exception {
        KeySecretCreateDTO dto = KeySecretCreateDTO.builder()
            .keyName("手机号FPE")
            .keyType("ENCRYPTION")
            .algorithm("FF1")
            .keyLength(128)
            .genType("SYSTEM")
            .build();

        when(keySecretAppService.createKey(any(KeySecretCreateDTO.class))).thenReturn(7001L);

        mockMvc.perform(post("/api/v1/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(7001L));
    }

    @Test
    @DisplayName("POST /api/v1/keys/{id}/reveal - 查看明文密钥返回解密结果")
    void testRevealPlaintext() throws Exception {
        when(keySecretAppService.revealKeyPlaintext(7001L)).thenReturn("plain_secret_text_123");

        mockMvc.perform(post("/api/v1/keys/7001/reveal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("plain_secret_text_123"));
    }

    @Test
    @DisplayName("POST /api/v1/keys/{id}/transfer - 转交责任人成功")
    void testTransferOwner() throws Exception {
        KeyTransferDTO dto = KeyTransferDTO.builder().newOwner("zhangsan").build();

        mockMvc.perform(post("/api/v1/keys/7001/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/keys/{id}/task-references - 查询任务引用记录")
    void testListTaskReferences() throws Exception {
        KeyTaskReferenceVO vo = KeyTaskReferenceVO.builder()
                .id(9101L)
                .keyId(7001L)
                .taskName("核心结算加密")
                .sectorName("资管板块")
                .projectName("清算项目")
                .taskType("DYNAMIC_MASK")
                .operationType("ENCRYPT")
                .owner("admin")
                .lastExecutedAt(LocalDateTime.now())
                .build();

        when(keySecretAppService.listTaskReferences(7001L)).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/v1/keys/7001/task-references"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskName").value("核心结算加密"));
    }

    @Test
    @DisplayName("DELETE /api/v1/keys/{id} - 存在引用时返回 409 Conflict")
    void testDeleteKey_Conflict() throws Exception {
        doThrow(new DataSecurityException("KEY_IN_USE", "密钥正被引用，禁止删除！"))
            .when(keySecretAppService).deleteKey(7001L);

        mockMvc.perform(delete("/api/v1/keys/7001"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("KEY_IN_USE"));
    }
}
