package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeySecretVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String keyName;
    private String keyType; // HASH / ENCRYPTION
    private String algorithm;
    private Integer keyLength;
    private String genType; // SYSTEM / CUSTOM
    private Boolean ownerOnly;
    private String publicKeyValue;
    private String description;
    private String owner;
    private String status;
    private Integer referencedRulesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
