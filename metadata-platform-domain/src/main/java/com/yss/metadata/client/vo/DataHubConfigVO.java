package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * DataHub 导出目标配置视图（IntegrationVO.datahub）。
 */
@Getter
@Setter
public class DataHubConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** DataHub 导出目标地址 */
    private String endpoint;
}
