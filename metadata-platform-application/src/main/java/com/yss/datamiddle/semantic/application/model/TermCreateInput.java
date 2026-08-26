package com.yss.datamiddle.semantic.application.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 新建术语用例输入。
 */
@Getter
@Builder
public class TermCreateInput {

    private final String name;

    private final List<String> aliases;

    private final String definition;

    private final String description;

    private final String owner;
}
