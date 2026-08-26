package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPackageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String projectId;
    private String projectName;
    private String projectCode;
    private String engineType;
    private String packageVersion;
    private String status; // INSTALLED, NOT_INSTALLED, UPGRADABLE
    private Integer authorizedCount;
    private List<String> authorizedFunctions;
    private String installedAt;
    private String installedBy;
}
