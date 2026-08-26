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
public class KeyPermission {
    private Long id;
    private Long keyId;
    private String granteeType; // USER / ROLE
    private String granteeId;
    private String granteeName;
    private String permissionType; // USE / MANAGE
    private String grantedBy;
    private LocalDateTime grantedAt;
}
