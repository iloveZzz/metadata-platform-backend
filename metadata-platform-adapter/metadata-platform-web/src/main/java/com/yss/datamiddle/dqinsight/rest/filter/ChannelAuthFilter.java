package com.yss.datamiddle.dqinsight.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelCredentialStore;
import com.yss.datamiddle.dqinsight.domain.model.ChannelCredential;
import com.yss.datamiddle.dqinsight.rest.exception.ErrorResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

/**
 * 通道级 Token 认证中间件（B1，POST /api/dq/results 认证前置）。
 *
 * <p>按请求携带的通道 Token 经 ChannelCredentialStore 端口读取对应通道凭证并校验（每通道独立 AK/SK，
 * SB-09 基线）；认证失败返回 422 err.dq.auth.invalid（错误分类 auth 一致）且错误信息脱敏（C19，
 * 不泄露凭证）。凭证存储（dq_channel 表 / 解密实现）由切片 04 落地，本切片以端口 + 测试 fixture
 * 验证认证与脱敏逻辑（合同 seam_deferred）。</p>
 *
 * <p>凭证存储缺失时 fail-closed（拒绝全部写入），保证安全缺省；`dq.security.channel-auth.enabled`
 * 可关闭作为紧急回退（BAC 风险 / 回滚约束）。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ChannelAuthFilter extends OncePerRequestFilter {

    /** 认证后写入 request 的通道 ID 属性名（Controller 经 @RequestAttribute 读取） */
    public static final String CHANNEL_ID_ATTRIBUTE = "dqChannelId";

    /** 受保护路径（冻结契约 POST /api/dq/results） */
    public static final String RESULTS_PATH = "/api/dq/results";

    private final ObjectProvider<ChannelCredentialStore> credentialStoreProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length()) : uri;
        return !RESULTS_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        ChannelCredentialStore store = credentialStoreProvider.getIfAvailable();
        if (token == null || store == null) {
            reject(response);
            return;
        }
        Optional<ChannelCredential> credential =
                store.findByTokenFingerprint(TokenFingerprint.sha256Hex(token));
        if (!credential.isPresent()) {
            reject(response);
            return;
        }
        request.setAttribute(CHANNEL_ID_ATTRIBUTE, credential.get().getChannelId());
        chain.doFilter(request, response);
    }

    /**
     * 认证失败响应（脱敏：不记录 / 不回显 Token 与指纹）。
     */
    private void reject(HttpServletResponse response) throws IOException {
        log.warn("通道认证失败：{}（错误信息已脱敏）", RESULTS_PATH);
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponseVO body = ErrorResponseVO.of(
                DqErrorCodes.AUTH_INVALID, "通道认证失败：无效或缺失的通道 Token",
                Collections.singletonList(FieldErrorItem.of("Authorization",
                        DqErrorCodes.AUTH_INVALID, "无效的通道 Token")));
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * 提取通道 Token：接受 "Bearer &lt;token&gt;" 或裸 token（apiKey / header Authorization）。
     */
    private static String extractToken(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String token = trimmed.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return trimmed;
    }
}
