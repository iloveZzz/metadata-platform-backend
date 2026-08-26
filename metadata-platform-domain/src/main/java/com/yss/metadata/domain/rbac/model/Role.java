package com.yss.metadata.domain.rbac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 角色（数据架构 Role：id/name/scope + role_domain N:M 数据域绑定）。
 *
 * <p>删除前 refs（role_domain 绑定数）=0 才可删（数据架构规则）；name 唯一。
 * 用户-角色绑定 seam-deferred（无用户表，用户管理在平台外，slice 06 登记）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 角色名（唯一） */
    private String name;

    /** 角色范围（范围描述文本，如"交易/客户/财务域"；精确绑定见 role_domain） */
    private String scope;
}
