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
@TableName("sec_key_permission")
public class KeyPermissionPO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("key_id")
    private Long keyId;

    @TableField("grantee_type")
    private String granteeType;

    @TableField("grantee_id")
    private String granteeId;

    @TableField("grantee_name")
    private String granteeName;

    @TableField("permission_type")
    private String permissionType;

    @TableField("granted_by")
    private String grantedBy;

    @TableField("granted_at")
    private LocalDateTime grantedAt;
}
