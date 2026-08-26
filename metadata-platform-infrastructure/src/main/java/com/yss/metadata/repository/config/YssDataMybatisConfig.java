package com.yss.metadata.repository.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 元数据平台 MyBatis-Plus Mapper 扫描与插件配置。
 *
 * <p>扫描本服务 repository 包的持久化仓库接口（BasePlusRepository 通用 Mapper 接入）；
 * 注册分页插件（PaginationInnerInterceptor），支撑资产目录分页查询
 * （WU-02-01 PageQuery 分页；selectPage 依赖该插件生成 LIMIT 与 count）。</p>
 */
@Configuration
@MapperScan(basePackages = "com.yss.metadata.repository", markerInterface = BaseMapper.class)
public class YssDataMybatisConfig {
}
