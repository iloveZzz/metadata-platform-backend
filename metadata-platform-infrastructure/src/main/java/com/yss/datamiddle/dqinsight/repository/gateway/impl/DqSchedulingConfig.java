package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 进程内调度开关（切片 02 过期流转低频调度，BAC 风险 / 回滚约束；基础设施持有调度器）。
 */
@Configuration
@EnableScheduling
public class DqSchedulingConfig {
}
