package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNodeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String nodeName;
    private Integer depthLevel;
    private String visibility;
    @Builder.Default
    private List<String> admins = new ArrayList<>();
    private String description;
    @Builder.Default
    private Integer categoryCount = 0;
    @Builder.Default
    private List<CategoryTreeNodeVO> children = new ArrayList<>();
}
