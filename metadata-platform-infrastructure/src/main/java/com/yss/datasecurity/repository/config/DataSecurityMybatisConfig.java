package com.yss.datasecurity.repository.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据安全中心 MyBatis-Plus Mapper 扫描与插件配置。
 *
 * <p>扫描本服务 repository.mapper 包下的 BaseMapper 接口；
 * 注册分页插件（PaginationInnerInterceptor），支撑分类、规则、识别任务、白名单、审计等分页查询。</p>
 */
@Configuration
@MapperScan(basePackages = "com.yss.datasecurity.repository.mapper", markerInterface = BaseMapper.class)
public class DataSecurityMybatisConfig {
    // 由 yss-component-mybatis-starter 的 MapperConfiguration 统一管理分页拦截器，避免双重 LIMIT 注入
}
