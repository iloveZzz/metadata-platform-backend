package com.yss.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.datamiddleds.client.annotation.EnableDataMiddleDsClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * metadata-platform 启动类。
 *
 * <p>YSS DDD 多模块后端骨架引导入口。全域业务切片（连接器采集、资产目录、血缘、
 * 分级分类、集成互导、RBAC、数据质量、语义层、AI上下文/MCP Server、数据安全中心、智能找数自动打标）统一装配。</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.yss.metadata",
        "com.yss.datamiddle",
        "com.yss.datasecurity",
        "com.yss.smartdiscovery"
})
@EnableDataMiddleDsClient
@MapperScan(
        basePackages = {
                "com.yss.metadata.repository",
                "com.yss.datamiddle.semantic.infrastructure.repository.mapper",
                "com.yss.datamiddle.dqinsight.repository",
                "com.yss.datamiddle.aicontextlayer.repository",
                "com.yss.datamiddle.smartgovernance.infrastructure.repository.mapper",
                "com.yss.datasecurity.infrastructure.repository.mapper",
                "com.yss.smartdiscovery.infrastructure.persistence"
        },
        markerInterface = BaseMapper.class)
public class MetadataPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataPlatformApplication.class, args);
    }
}
