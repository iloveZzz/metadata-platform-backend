package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.datamiddle.dqinsight.domain.gateway.CatalogAclGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 资产对齐防腐层实现：只读消费主平台冻结资产 API（GET /api/assets/{id}、GET /api/assets）。
 *
 * <p>200 → FOUND（含名称 / 域 / 类型快照）；404 → NOT_FOUND（挂待关联队列）；超时 / 连接失败 →
 * NETWORK_FAILURE（network 分类 422）。快照缓存与人工映射复用补强在切片 04（合同 seam_deferred）。
 * countVisibleTargetAssets：覆盖率分母（SB-07）只读消费 GET /api/assets（PageResult.totalCount），
 * 防腐层不可用按 0 处理（targetAssetCount=0 → 覆盖率按 0 表达，切片 03 人工审查点）。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CatalogAclGatewayImpl implements CatalogAclGateway {

    private static final String ASSET_API_PATH = "/api/assets/{id}";

    private static final String ASSETS_API_PATH = "/api/assets";

    /** 目标资产数 TTL 缓存窗口（数据架构 §9「防腐层缓存目标资产数」；仅缓存无 domain 收敛的全量口径） */
    private static final long TARGET_COUNT_TTL_MILLIS = 60_000L;

    private final RestTemplate restTemplate;
    private final DqCatalogApiProperties properties;
    private final ObjectMapper objectMapper;

    private volatile long totalCountFetchedAt = 0L;
    private volatile int totalCountCache = 0;

    @Override
    public AssetLookupResult lookupAsset(String assetId) {
        String url = properties.getBaseUrl() + ASSET_API_PATH;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, assetId);
            if (response.getStatusCode().is2xxSuccessful()) {
                return AssetLookupResult.found(assetId, parseSnapshot(assetId, response.getBody()));
            }
            log.warn("资产校验非预期状态: assetId={}, status={}", assetId, response.getStatusCode());
            return AssetLookupResult.networkFailure(assetId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return AssetLookupResult.notFound(assetId);
            }
            log.warn("资产校验客户端错误: assetId={}, status={}", assetId, e.getStatusCode());
            return AssetLookupResult.networkFailure(assetId);
        } catch (RestClientException e) {
            // 不记录凭证 / 请求体，仅记录可脱敏原因
            log.warn("资产校验服务不可用（网络超时 / 连接失败）: assetId={}", assetId);
            return AssetLookupResult.networkFailure(assetId);
        }
    }

    @Override
    public int countVisibleTargetAssets(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - totalCountFetchedAt < TARGET_COUNT_TTL_MILLIS) {
                return totalCountCache;
            }
            int count = fetchTargetCount(null);
            totalCountFetchedAt = now;
            totalCountCache = count;
            return count;
        }
        return fetchTargetCount(domain.trim());
    }

    /**
     * 只读消费 GET /api/assets（page=1&size=1）读取 PageResult.totalCount；失败按 0（覆盖率按 0 表达）。
     */
    private int fetchTargetCount(String domain) {
        String url = properties.getBaseUrl() + ASSETS_API_PATH + "?page=1&size=1"
                + (domain == null ? "" : "&domain=" + urlEncode(domain));
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseTotalCount(response.getBody());
            }
            log.warn("目标资产数查询非预期状态: domain={}, status={}", domain, response.getStatusCode());
        } catch (RestClientException e) {
            // 不记录 URL 查询串（可能含域参数），仅记录可脱敏原因
            log.warn("目标资产数查询服务不可用（网络超时 / 连接失败）: domain={}", domain);
        }
        return 0;
    }

    private int parseTotalCount(String body) {
        if (body == null || body.isEmpty()) {
            return 0;
        }
        try {
            JsonNode total = objectMapper.readTree(body).path("totalCount");
            return total.isNumber() ? total.asInt() : 0;
        } catch (Exception e) {
            log.warn("目标资产数响应体解析失败，按 0 处理");
            return 0;
        }
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * 从 YSS Result 响应体提取 data 快照字段（name / domain / type；字段缺失容忍）。
     */
    private AssetSnapshot parseSnapshot(String assetId, String body) {
        AssetSnapshot.AssetSnapshotBuilder builder = AssetSnapshot.builder().assetId(assetId);
        if (body != null && !body.isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.path("data");
                builder.assetName(textOrNull(data.get("name")));
                builder.domain(textOrNull(data.get("domain")));
                builder.assetType(textOrNull(data.get("type")));
            } catch (Exception e) {
                log.warn("资产响应体解析失败，仅保留存在性: assetId={}", assetId);
            }
        }
        return builder.build();
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() || !node.isTextual() ? null : node.asText();
    }
}
