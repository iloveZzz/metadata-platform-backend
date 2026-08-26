package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeySecretUpdateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private String keyType;
    private String algorithm;
    private Integer keyLength;
    private String genType;
    private String customKeyValue;
    private String publicKey;
    private String privateKey;
    private Boolean ownerOnly;

    @Size(max = 128, message = "描述长度不能超过128字符")
    private String description;
}
