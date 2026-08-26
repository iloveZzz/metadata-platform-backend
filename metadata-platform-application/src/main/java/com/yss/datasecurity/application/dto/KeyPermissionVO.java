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
public class KeyPermissionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long keyId;
    private String granteeType;
    private String granteeId;
    private String granteeName;
    private String permissionType;
    private String grantedBy;
    private LocalDateTime grantedAt;
}
