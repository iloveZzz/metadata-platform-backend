package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode {
    private Long id;
    private Long parentId;
    private String nodeName;
    private String nodePath;
    private Integer depthLevel;
    private String visibility; // PUBLIC / ADMIN_ONLY
    @Builder.Default
    private List<String> admins = new ArrayList<>();
    private String description;
    private Integer sortOrder;
    private Integer categoryCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CategoryTreeNode> children = new ArrayList<>();
}
