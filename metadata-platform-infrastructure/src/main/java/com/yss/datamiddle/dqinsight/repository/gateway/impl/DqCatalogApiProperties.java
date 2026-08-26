package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 主平台资产 API 消费配置（只读消费冻结资产 API，C17；不写主平台表与冻结契约）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dq.catalog-api")
public class DqCatalogApiProperties {

    /** 主平台 API 基础地址（部署配置；切片 04 与部署联调时定稿） */
    private String baseUrl = "";
}
