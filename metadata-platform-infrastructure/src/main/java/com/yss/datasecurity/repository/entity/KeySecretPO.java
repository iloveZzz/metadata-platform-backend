package com.yss.datasecurity.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("sec_key_secret")
public class KeySecretPO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("key_name")
    private String keyName;

    @TableField("key_type")
    private String keyType;

    @TableField("algorithm")
    private String algorithm;

    @TableField("key_length")
    private Integer keyLength;

    @TableField("gen_type")
    private String genType;

    @TableField("owner_only")
    private Boolean ownerOnly;

    @TableField("encrypted_key_value")
    private String encryptedKeyValue;

    @TableField("public_key_value")
    private String publicKeyValue;

    @TableField("description")
    private String description;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private String status;

    @TableField("referenced_rules_count")
    private Integer referencedRulesCount;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
