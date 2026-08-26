package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yss.datamiddle.dqinsight.domain.model.AssetLookupResult;
import com.yss.datamiddle.dqinsight.domain.model.AssetSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防腐层消费侧契约对齐测试（WU2 / cross_repo：GET /api/assets/{id} 响应映射）。
 *
 * <p>以 JDK 内嵌 HTTP Server 模拟主平台冻结资产 API：200 → FOUND（含快照）；404 → NOT_FOUND；
 * 连接失败（端口未监听）→ NETWORK_FAILURE。</p>
 */
class CatalogAclGatewayTest {

    private HttpServer server;
    private int port;
    private CatalogAclGatewayImpl gateway;
    private int assetsHitCount;
    private String lastAssetsQuery;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/assets/asset-found", exchange -> {
            String json = "{\"code\":\"0\",\"message\":\"ok\",\"data\":{\"id\":\"asset-found\","
                    + "\"name\":\"用户表\",\"domain\":\"交易域\",\"type\":\"table\"}}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/api/assets/asset-missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/api/assets/asset-empty-snapshot", exchange -> {
            byte[] body = "{\"code\":\"0\",\"message\":\"ok\",\"data\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        // 覆盖率分母：GET /api/assets 返回 PageResult.totalCount（冻结信封）；记录查询串验证 domain 收敛
        server.createContext("/api/assets", exchange -> {
            assetsHitCount++;
            lastAssetsQuery = exchange.getRequestURI().getQuery();
            String json = "{\"success\":true,\"code\":\"DM-A0001\",\"totalCount\":42,"
                    + "\"pageIndex\":1,\"pageSize\":1,\"data\":[]}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        port = server.getAddress().getPort();
        DqCatalogApiProperties properties = new DqCatalogApiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + port);
        gateway = new CatalogAclGatewayImpl(new RestTemplate(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void assetFoundMapsToFoundWithSnapshot() {
        AssetLookupResult result = gateway.lookupAsset("asset-found");

        assertThat(result.getType()).isEqualTo(AssetLookupResult.LookupType.FOUND);
        AssetSnapshot snapshot = result.getSnapshot();
        assertThat(snapshot.getAssetId()).isEqualTo("asset-found");
        assertThat(snapshot.getAssetName()).isEqualTo("用户表");
        assertThat(snapshot.getDomain()).isEqualTo("交易域");
        assertThat(snapshot.getAssetType()).isEqualTo("table");
    }

    @Test
    void assetNotFoundMapsToPendingQueueOutcome() {
        AssetLookupResult result = gateway.lookupAsset("asset-missing");

        assertThat(result.getType()).isEqualTo(AssetLookupResult.LookupType.NOT_FOUND);
    }

    @Test
    void emptySnapshotKeepsExistenceSemantics() {
        AssetLookupResult result = gateway.lookupAsset("asset-empty-snapshot");

        assertThat(result.getType()).isEqualTo(AssetLookupResult.LookupType.FOUND);
        assertThat(result.getSnapshot().getAssetName()).isNull();
    }

    @Test
    void connectionFailureMapsToNetworkFailure() {
        // 端口未监听 → 连接失败
        DqCatalogApiProperties properties = new DqCatalogApiProperties();
        properties.setBaseUrl("http://localhost:1");
        CatalogAclGatewayImpl unreachable = new CatalogAclGatewayImpl(new RestTemplate(), properties,
                new ObjectMapper());

        AssetLookupResult result = unreachable.lookupAsset("asset-x");

        assertThat(result.getType()).isEqualTo(AssetLookupResult.LookupType.NETWORK_FAILURE);
    }

    @Test
    void countVisibleTargetAssetsReadsTotalCountFromAssetsPage() {
        int count = gateway.countVisibleTargetAssets(null);

        assertThat(count).isEqualTo(42);
        assertThat(assetsHitCount).isEqualTo(1);
    }

    @Test
    void countVisibleTargetAssetsCachesWithinTtlWindow() {
        gateway.countVisibleTargetAssets(null);
        int second = gateway.countVisibleTargetAssets(null);

        assertThat(second).isEqualTo(42);
        assertThat(assetsHitCount).isEqualTo(1); // TTL 窗口内命中缓存，不重复调用
    }

    @Test
    void countVisibleTargetAssetsPassesDomainParamWhenProvided() {
        gateway.countVisibleTargetAssets("交易域");

        assertThat(assetsHitCount).isEqualTo(1);
        assertThat(lastAssetsQuery).isNotNull().contains("domain=");
    }

    @Test
    void countVisibleTargetAssetsReturnsZeroOnNetworkFailure() {
        DqCatalogApiProperties properties = new DqCatalogApiProperties();
        properties.setBaseUrl("http://localhost:1");
        CatalogAclGatewayImpl unreachable = new CatalogAclGatewayImpl(new RestTemplate(), properties,
                new ObjectMapper());

        assertThat(unreachable.countVisibleTargetAssets(null)).isZero();
    }
}
