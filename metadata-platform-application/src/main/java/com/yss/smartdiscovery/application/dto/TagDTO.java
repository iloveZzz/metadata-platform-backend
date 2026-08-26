package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO implements Serializable {
    private String id;
    private String name;
    private String code;
    private String categoryCode;
    private String categoryName;
    private String colorToken;
    private String description;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
