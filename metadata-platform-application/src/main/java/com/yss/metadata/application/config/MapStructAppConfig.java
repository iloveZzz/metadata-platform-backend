package com.yss.metadata.application.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 应用层 MapStruct 全局通用配置（符合 ADR-0008 规范）。
 *
 * <p>特性：
 * 1. componentModel = "spring"：统一 Spring 容器管理；
 * 2. unmappedTargetPolicy = ReportingPolicy.IGNORE：VO 视图裁剪允许忽略未映射字段；
 * 3. nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS：开启非空检查；
 * 4. nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE：默认忽略 null。
 * </p>
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MapStructAppConfig {
}
