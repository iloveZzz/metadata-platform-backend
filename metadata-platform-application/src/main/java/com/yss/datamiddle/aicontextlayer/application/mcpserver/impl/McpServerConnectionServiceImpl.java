package com.yss.datamiddle.aicontextlayer.application.mcpserver.impl;

import com.yss.datamiddle.aicontextlayer.application.mcpserver.McpServerConnectionService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAttempt;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ConnectionAuthenticator;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * MCP 连接建立用例实现：鉴权与会话建立编排（核心领域规则在 Domain，本层只做用例编排）。
 */
@Service
@RequiredArgsConstructor
public class McpServerConnectionServiceImpl implements McpServerConnectionService {

    private final ConnectionAuthenticator connectionAuthenticator;

    @Override
    public McpSession establishConnection(ConnectionAttempt attempt) {
        Instant now = Instant.now();
        return connectionAuthenticator.authenticate(attempt, now);
    }
}
