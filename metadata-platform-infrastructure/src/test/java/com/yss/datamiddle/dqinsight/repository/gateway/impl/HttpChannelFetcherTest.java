package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.sun.net.httpserver.HttpServer;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FetchResult;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
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
 * 通道拉取取数契约测试（DQI-SLICE-04-WU2，ChannelFetchPort）。
 *
 * <p>以 JDK 内嵌 HTTP Server 模拟外部 DQ 工具拉取端点：200 → 成功（内容 + 按格式类型映射内容类型）；
 * 401 → auth 分类；连接失败 / 未配置地址 → network 分类。</p>
 */
class HttpChannelFetcherTest {

    private HttpServer server;
    private int port;
    private HttpChannelFetcher fetcher;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/pull/1", exchange -> {
            byte[] body = "{\"sourceTool\":\"great-expectations\",\"batchNo\":\"pull-batch-1\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/pull/2", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
        DqPullProperties properties = new DqPullProperties();
        properties.setBaseUrl("http://localhost:" + port);
        fetcher = new HttpChannelFetcher(new RestTemplate(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void successfulFetchReturnsContentWithJsonContentType() {
        IngestionChannel channel = channel(1L, FormatType.GE);

        FetchResult result = fetcher.fetch(channel);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).contains("pull-batch-1");
        assertThat(result.getContentType()).isEqualTo("application/json");
    }

    @Test
    void csvFormatMapsToTextCsvContentType() {
        IngestionChannel channel = channel(1L, FormatType.CSV);

        FetchResult result = fetcher.fetch(channel);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContentType()).isEqualTo("text/csv");
    }

    @Test
    void unauthorizedClassifiedAsAuthFailure() {
        IngestionChannel channel = channel(2L, FormatType.GE);

        FetchResult result = fetcher.fetch(channel);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.AUTH);
    }

    @Test
    void connectionFailureClassifiedAsNetwork() {
        DqPullProperties properties = new DqPullProperties();
        properties.setBaseUrl("http://localhost:1");
        HttpChannelFetcher unreachable = new HttpChannelFetcher(new RestTemplate(), properties);

        FetchResult result = unreachable.fetch(channel(9L, FormatType.GE));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.NETWORK);
    }

    @Test
    void unconfiguredBaseUrlFailsAsNetwork() {
        DqPullProperties properties = new DqPullProperties();
        HttpChannelFetcher unconfigured = new HttpChannelFetcher(new RestTemplate(), properties);

        FetchResult result = unconfigured.fetch(channel(9L, FormatType.GE));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.NETWORK);
    }

    private static IngestionChannel channel(Long id, FormatType formatType) {
        IngestionChannel channel = IngestionChannel.create("拉取通道", ChannelType.SCHEDULED_PULL,
                "0 * * * * *", formatType, null, true);
        channel.setId(id);
        return channel;
    }
}
