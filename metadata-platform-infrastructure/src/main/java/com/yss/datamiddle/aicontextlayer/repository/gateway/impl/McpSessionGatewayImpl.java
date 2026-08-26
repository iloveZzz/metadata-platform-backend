package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSessionStatus;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.McpSessionGateway;
import com.yss.datamiddle.aicontextlayer.repository.McpSessionRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.McpSessionConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.McpSessionPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import com.yss.datamiddle.aicontextlayer.repository.util.TimeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class McpSessionGatewayImpl implements McpSessionGateway {

    private final McpSessionRepository mcpSessionRepository;
    private final McpSessionConvertor mcpSessionConvertor;

    @Override
    @Transactional
    public String addMcpSession(McpSession entity) {
        McpSessionPO po = mcpSessionConvertor.toPO(entity);
        mcpSessionRepository.insert(po);
        return po.getId();
    }

    @Override
    public boolean updateMcpSession(McpSession entity) {
        return mcpSessionRepository.updateById(mcpSessionConvertor.toPO(entity)) > 0;
    }

    @Override
    public boolean deleteMcpSession(String id) {
        return mcpSessionRepository.deleteById(id) > 0;
    }

    @Override
    public Optional<McpSession> getMcpSessionById(String id) {
        return Optional.ofNullable(mcpSessionRepository.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<McpSession> pageMcpSession(PageQuery query) {
        LambdaQueryWrapper<McpSessionPO> wrapper = Wrappers.lambdaQuery(McpSessionPO.class);
        
        IPage<McpSessionPO> result = mcpSessionRepository.selectPage(PageUtil.page(query), wrapper);
        List<McpSession> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public int countActiveSessions(String agentId) {
        Long count = mcpSessionRepository.selectCount(Wrappers.<McpSessionPO>lambdaQuery()
            .eq(McpSessionPO::getAgentId, agentId)
            .eq(McpSessionPO::getStatus, McpSessionStatus.ACTIVE.name()));
        return count == null ? 0 : Math.toIntExact(count);
    }

    @Override
    @Transactional
    public int forceTerminateByCredential(String agentId, String credentialVersion, Instant terminatedAt) {
        // 单聚合状态流转：ACTIVE → TERMINATED（吊销强制断开，SEC-05；数据架构 §6.1 会话路径）
        return mcpSessionRepository.update(null, Wrappers.<McpSessionPO>lambdaUpdate()
            .eq(McpSessionPO::getAgentId, agentId)
            .eq(McpSessionPO::getCredentialVersion, credentialVersion)
            .eq(McpSessionPO::getStatus, McpSessionStatus.ACTIVE.name())
            .set(McpSessionPO::getStatus, McpSessionStatus.TERMINATED.name())
            .set(McpSessionPO::getTerminatedAt, TimeSupport.toLocalDateTime(terminatedAt)));
    }

    @Override
    @Transactional
    public int reclaimExpired(Instant now) {
        // 单聚合状态流转：ACTIVE → EXPIRED（过期 / 空闲回收，REC-05；幂等，重复执行返回 0）
        return mcpSessionRepository.update(null, Wrappers.<McpSessionPO>lambdaUpdate()
            .eq(McpSessionPO::getStatus, McpSessionStatus.ACTIVE.name())
            .le(McpSessionPO::getExpiresAt, TimeSupport.toLocalDateTime(now))
            .set(McpSessionPO::getStatus, McpSessionStatus.EXPIRED.name()));
    }

    private McpSession toDomain(McpSessionPO source) {
        return mcpSessionConvertor.toDomain(source);
    }
}
