package com.yss.datamiddle.semantic.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynonymSetCreateInput {
    private String name;
    private String canonical;
    private List<String> words;
    private Long termId;
}
