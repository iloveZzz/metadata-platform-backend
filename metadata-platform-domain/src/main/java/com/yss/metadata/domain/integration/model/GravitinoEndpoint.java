package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Gravitino 端点值对象（防腐层输入；外部模型隔离，仅暴露端点与认证）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GravitinoEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Gravitino 端点地址 */
    private String endpoint;

    /** 认证令牌（测试连接场景明文；持久化走加密引用） */
    private String authToken;
}
