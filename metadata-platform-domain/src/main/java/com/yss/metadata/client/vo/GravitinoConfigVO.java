package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Gravitino 上游配置视图（IntegrationVO.gravitino）。
 */
@Getter
@Setter
public class GravitinoConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Gravitino 端点地址 */
    private String endpoint;

    /** 是否启用 */
    private Boolean enabled;

    /** 最近测试连接结果（时间/状态/分类；未测试为空） */
    private String lastTest;
}
