package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentDomainGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentDomainMappingServiceTest {

    private final AgentDomainGateway gateway = Mockito.mock(AgentDomainGateway.class);
    private final AgentDomainMappingService service = new AgentDomainMappingService(gateway);

    @Test
    @DisplayName("合法 Agent 授权数据域校验通过")
    void authorizedDomainsAccessible() {
        assertTrue(service.isDomainAccessible("agent-001", "default"));
        assertTrue(service.isDomainAccessible("agent-001", "public"));
    }

    @Test
    @DisplayName("未授权数据域访问被拒绝（SEC-01）")
    void unauthorizedDomainRejected() {
        assertFalse(service.isDomainAccessible("agent-001", "confidential_financial"));
        assertFalse(service.isDomainAccessible("agent-001", "restricted_hr"));
    }

    @Test
    @DisplayName("空参数安全拦截")
    void emptyParamsRejected() {
        assertFalse(service.isDomainAccessible(null, "public"));
        assertFalse(service.isDomainAccessible("agent-001", null));
        assertFalse(service.isDomainAccessible("", ""));
    }
}
