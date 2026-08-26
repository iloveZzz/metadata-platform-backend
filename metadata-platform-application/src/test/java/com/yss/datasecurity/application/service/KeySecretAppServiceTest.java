package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.KeySecretConvertor;
import com.yss.datasecurity.application.dto.KeyPermissionDTO;
import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretUpdateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;
import com.yss.datasecurity.application.service.impl.KeySecretAppServiceImpl;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.KeySecretGateway;
import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.domain.model.KeyReference;
import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.domain.model.KeyTaskReference;
import com.yss.datasecurity.domain.service.KeyEnvelopeCryptoEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeySecretAppServiceTest {

    @Mock
    private KeySecretGateway keySecretGateway;

    private KeySecretAppService keySecretAppService;

    @BeforeEach
    void setUp() {
        keySecretAppService = new KeySecretAppServiceImpl(
            keySecretGateway,
            new KeyEnvelopeCryptoEngine(),
            org.mapstruct.factory.Mappers.getMapper(KeySecretConvertor.class)
        );
    }

    @Test
    @DisplayName("测试注册安全密钥 - 自动生成密钥并进行信封二次加密落盘")
    void testCreateKey_AutoGenerate() {
        KeySecretCreateDTO dto = KeySecretCreateDTO.builder()
            .keyName("生产FPE密钥")
            .keyType("ENCRYPTION")
            .algorithm("FF1")
            .keyLength(128)
            .genType("SYSTEM")
            .description("用于客户手机号格式保留加密")
            .build();

        when(keySecretGateway.findByName("生产FPE密钥")).thenReturn(Optional.empty());
        when(keySecretGateway.save(any(KeySecret.class))).thenAnswer(invocation -> {
            KeySecret k = invocation.getArgument(0);
            assertNotNull(k.getEncryptedKeyValue());
            assertTrue(k.getEncryptedKeyValue().startsWith("ENC(v1:"));
            k.setId(7001L);
            return k;
        });

        Long keyId = keySecretAppService.createKey(dto);
        assertEquals(7001L, keyId);
    }

    @Test
    @DisplayName("测试查看明文密钥 - 解密信封密文并记录高危审计")
    void testRevealPlaintext_Success() {
        Long keyId = 7001L;
        KeyEnvelopeCryptoEngine engine = new KeyEnvelopeCryptoEngine();
        String plaintext = "my_custom_secret_key_123456";
        String encrypted = engine.encryptUnderMasterKey(plaintext);

        KeySecret key = KeySecret.builder()
            .id(keyId)
            .keyName("测试密钥")
            .keyType("ENCRYPTION")
            .algorithm("AES")
            .keyLength(256)
            .encryptedKeyValue(encrypted)
            .build();

        when(keySecretGateway.findById(keyId)).thenReturn(Optional.of(key));

        String result = keySecretAppService.revealKeyPlaintext(keyId);
        assertEquals(plaintext, result);
    }

    @Test
    @DisplayName("测试转交密钥负责人")
    void testTransferOwner_Success() {
        Long keyId = 7001L;
        KeySecret key = KeySecret.builder().id(keyId).keyName("测试密钥").owner("admin").build();
        when(keySecretGateway.findById(keyId)).thenReturn(Optional.of(key));

        keySecretAppService.transferOwner(keyId, "zhangsan");
        assertEquals("zhangsan", key.getOwner());
        verify(keySecretGateway).update(key);
    }

    @Test
    @DisplayName("测试授予密钥权限与回收")
    void testPermissionGrantAndRevoke() {
        Long keyId = 7001L;
        KeySecret key = KeySecret.builder().id(keyId).keyName("测试密钥").build();
        when(keySecretGateway.findById(keyId)).thenReturn(Optional.of(key));

        KeyPermissionDTO permDto = KeyPermissionDTO.builder()
                .granteeType("USER")
                .granteeId("u101")
                .granteeName("研发张三")
                .permissionType("USE")
                .build();

        when(keySecretGateway.savePermission(any(KeyPermission.class))).thenAnswer(inv -> {
            KeyPermission p = inv.getArgument(0);
            p.setId(901L);
            return p;
        });

        Long permId = keySecretAppService.grantPermission(keyId, permDto);
        assertEquals(901L, permId);

        keySecretAppService.revokePermission(keyId, 901L);
        verify(keySecretGateway).deletePermission(901L);
    }

    @Test
    @DisplayName("测试删除密钥 - 存在脱敏规则引用时强校验拦截抛出 409 KEY_IN_USE")
    void testDeleteKey_ConflictWhenReferenced() {
        Long keyId = 7001L;
        KeySecret key = KeySecret.builder().id(keyId).keyName("测试密钥").build();
        when(keySecretGateway.findById(keyId)).thenReturn(Optional.of(key));
        when(keySecretGateway.listReferences(keyId)).thenReturn(Collections.singletonList(
            KeyReference.builder().ruleId(8001L).ruleName("手机号脱敏规则").build()
        ));

        DataSecurityException ex = assertThrows(DataSecurityException.class, () -> keySecretAppService.deleteKey(keyId));
        assertEquals("KEY_IN_USE", ex.getCode());
    }

    @Test
    @DisplayName("测试删除密钥 - 无引用时成功删除")
    void testDeleteKey_Success() {
        Long keyId = 7002L;
        KeySecret key = KeySecret.builder().id(keyId).keyName("独立密钥").build();
        when(keySecretGateway.findById(keyId)).thenReturn(Optional.of(key));
        when(keySecretGateway.listReferences(keyId)).thenReturn(Collections.emptyList());
        when(keySecretGateway.listTaskReferences(keyId)).thenReturn(Collections.emptyList());

        keySecretAppService.deleteKey(keyId);
        verify(keySecretGateway).deleteById(keyId);
    }
}
