package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * DataHub 导出目标值对象（防腐层输入；外部模型隔离）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHubEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** DataHub 端点地址 */
    private String endpoint;

    /** 认证令牌（导出场景明文；持久化走加密引用） */
    private String authToken;
}
