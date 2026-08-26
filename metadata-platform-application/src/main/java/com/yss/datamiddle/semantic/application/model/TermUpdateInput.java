package com.yss.datamiddle.semantic.application.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 更新术语用例输入（携带乐观锁版本号）。
 */
@Getter
@Builder
public class TermUpdateInput {

    private final String name;

    private final List<String> aliases;

    private final String definition;

    private final String description;

    private final String owner;

    /** 乐观锁版本号（过期返回 409 VERSION_CONFLICT + 最新对象） */
    private final Integer version;
}
