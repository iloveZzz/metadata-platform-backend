package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.domain.gateway.ChannelFetchPort;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FetchResult;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 通道拉取取数实现（HTTP GET {dq.pull.base-url}/pull/{channelId}，返回原始结果内容）。
 *
 * <p>成功（2xx 且非空）→ 按通道格式类型映射内容类型（GE / API → application/json，CSV → text/csv），
 * 交由切片 01 接入管线入库；401 / 403 → auth 分类；其余非 2xx / 网络异常 → network 分类（脱敏）。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HttpChannelFetcher implements ChannelFetchPort {

    private static final String PULL_PATH = "/pull/";

    private final RestTemplate restTemplate;
    private final DqPullProperties properties;

    @Override
    public FetchResult fetch(IngestionChannel channel) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return FetchResult.failure(ErrorCategory.NETWORK, "拉取地址未配置（dq.pull.base-url）");
        }
        String url = baseUrl.trim() + PULL_PATH + channel.getId();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null && !response.getBody().isEmpty()) {
                return FetchResult.success(response.getBody(), contentTypeOf(channel.getFormatType()));
            }
            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || response.getStatusCode() == HttpStatus.FORBIDDEN) {
                return FetchResult.failure(ErrorCategory.AUTH, "拉取认证失败（HTTP " + response.getStatusCodeValue() + "）");
            }
            log.warn("拉取非预期响应: channelId={}, status={}", channel.getId(), response.getStatusCodeValue());
            return FetchResult.failure(ErrorCategory.NETWORK, "拉取响应非预期（HTTP " + response.getStatusCodeValue() + "）");
        } catch (HttpClientErrorException e) {
            // 4xx：401 / 403 → auth 分类；其余客户端错误 → network 分类
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return FetchResult.failure(ErrorCategory.AUTH, "拉取认证失败（HTTP " + e.getStatusCode().value() + "）");
            }
            log.warn("拉取客户端错误: channelId={}, status={}", channel.getId(), e.getStatusCode().value());
            return FetchResult.failure(ErrorCategory.NETWORK, "拉取响应非预期（HTTP " + e.getStatusCode().value() + "）");
        } catch (RestClientException e) {
            // 不记录 URL（可能含通道标识），仅记录可脱敏原因
            log.warn("拉取网络失败: channelId={}", channel.getId());
            return FetchResult.failure(ErrorCategory.NETWORK, "拉取网络失败（连接 / 超时）");
        }
    }

    private static String contentTypeOf(FormatType formatType) {
        return formatType == FormatType.CSV ? "text/csv" : "application/json";
    }
}
