package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 基础设施配置：防腐层 RestTemplate（连接 / 读取超时，供只读消费主平台冻结资产 API）+
 * Repository Mapper 扫描（dq_ 仓储，显式扫描避免依赖 yss.mybatis.mapper-scan 属性绑定时序）。
 */
@Configuration
@MapperScan(basePackages = "com.yss.datamiddle.dqinsight.repository", markerInterface = BaseMapper.class)
public class DqInfrastructureConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestTemplate dqCatalogRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build();
    }
}
