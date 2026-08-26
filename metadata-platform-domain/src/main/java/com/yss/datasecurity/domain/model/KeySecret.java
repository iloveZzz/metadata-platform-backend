package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeySecret {
    private Long id;
    private String keyName;
    private String keyType; // HASH / ENCRYPTION
    private String algorithm; // AES / DES / 3DES / SM2 / SM4 / RSA / FF1 / -
    private Integer keyLength; // 密钥位数 (128, 192, 256, 64, 112, 168, 1024, 2048, 4096)
    private String genType; // SYSTEM / CUSTOM
    private Boolean ownerOnly; // 是否仅负责人管理
    private String encryptedKeyValue; // 密文存储
    private String publicKeyValue; // 非对称公钥
    private String description;
    private String owner;
    private String status; // ACTIVE / EXPIRED / REVOKED
    private Integer referencedRulesCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
