package com.yss.datamiddle.semantic.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynonymExpansionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long synonymSetId;
    private String name;
    private String canonical;
    private List<String> words;
    private Long termId;
}
