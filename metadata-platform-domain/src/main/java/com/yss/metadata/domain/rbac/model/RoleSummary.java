package com.yss.metadata.domain.rbac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 角色摘要（列表展示：Role + refs 引用数）。
 *
 * <p>refs = 该角色 role_domain 数据域绑定数（删除前 refs=0 才可删；
 * 用户引用计数 seam-deferred，无用户表）。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 角色名（唯一） */
    private String name;

    /** 角色范围描述 */
    private String scope;

    /** 被引用数（role_domain 数据域绑定数） */
    private long refs;
}
