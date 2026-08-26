package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultMaskingPolicyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String securityGrade;
    private String algorithmType;
    private String description;
    private String updatedAt;
}
