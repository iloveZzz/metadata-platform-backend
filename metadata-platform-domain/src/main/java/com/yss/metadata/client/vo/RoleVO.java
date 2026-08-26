package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 角色视图对象（冻结 OpenAPI GET /api/roles 响应 data 项）。
 */
@Getter
@Setter
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 id */
    private String id;

    /** 角色名 */
    private String name;

    /** 角色范围描述 */
    private String scope;

    /** 被引用数（role_domain 数据域绑定数；用户引用 seam-deferred） */
    private long refs;
}
