package com.yss.datamiddle.dqinsight.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 通道级 Token 认证中间件注册（B1）。
 *
 * <p>`dq.security.channel-auth.enabled` 支持关闭作为紧急回退（BAC 风险 / 回滚约束；
 * matchIfMissing = true 默认开启，安全缺省）。</p>
 */
@Configuration
@ConditionalOnProperty(name = "dq.security.channel-auth.enabled", havingValue = "true", matchIfMissing = true)
public class ChannelAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<ChannelAuthFilter> channelAuthFilterRegistration(
            ObjectProvider<ChannelCredentialStore> credentialStoreProvider, ObjectMapper objectMapper) {
        ChannelAuthFilter filter = new ChannelAuthFilter(credentialStoreProvider, objectMapper);
        FilterRegistrationBean<ChannelAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns(ChannelAuthFilter.RESULTS_PATH);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
