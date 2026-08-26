package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeySecretCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 10, message = "密钥名称不能超过10个字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "密钥名称支持中文、英文、数字或下划线(_)")
    private String keyName;

    @NotBlank(message = "密钥类型不能为空")
    private String keyType; // HASH / ENCRYPTION

    private String algorithm; // AES / DES / 3DES / SM2 / SM4 / RSA / FF1 / -

    private Integer keyLength; // 128 / 192 / 256 / 64 / 112 / 168 / 1024 / 2048 / 4096

    private String genType; // SYSTEM / CUSTOM

    private String customKeyValue; // 自定义对称密钥或哈希盐值

    private String publicKey; // 自定义公钥

    private String privateKey; // 自定义私钥

    private Boolean ownerOnly; // 仅负责人管理

    @Size(max = 128, message = "描述长度不能超过128字符")
    private String description;
}
