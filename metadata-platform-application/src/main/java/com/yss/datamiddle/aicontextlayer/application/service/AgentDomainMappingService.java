package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AgentDomainGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Agent 身份 → 数据域映射服务（SEC-01 / IC-01 / 安全断言 1）。
 *
 * <p>校验 Agent 访问范围是否在其被授权的数据域内，确保下游调用凭据域 ⊆ Agent 数据域。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentDomainMappingService {

    private final AgentDomainGateway agentDomainGateway;

    /**
     * 校验指定 Agent 是否具备目标数据域的访问权限。
     *
     * @param agentId Agent 唯一标识
     * @param domainName 目标数据域
     * @return 是否有权访问
     */
    public boolean isDomainAccessible(String agentId, String domainName) {
        if (agentId == null || agentId.trim().isEmpty() || domainName == null || domainName.trim().isEmpty()) {
            return false;
        }
        Set<String> domains = getAuthorizedDomains(agentId);
        return domains.contains("*") || domains.contains(domainName.trim().toLowerCase());
    }

    /**
     * 获取 Agent 被授权的数据域集合。
     *
     * @param agentId Agent 唯一标识
     * @return 数据域集合
     */
    public Set<String> getAuthorizedDomains(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            return Collections.emptySet();
        }
        // MVP 默认授权公共域或通过 Gateway 加载
        Set<String> set = new HashSet<>();
        set.add("default");
        set.add("public");
        return Collections.unmodifiableSet(set);
    }
}
