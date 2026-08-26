package com.yss.metadata;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddleds.client.feign.DataSourceFeignClient;
import com.yss.metadata.client.vo.CollectorVO;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.rest.CollectorController;
import com.yss.metadata.rest.ConnectorController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 CollectorController 与 ConnectorController 的 list() 方法及 /api/connectors 端点在真实 Spring 上下文中正常装配与路由。
 */
@SpringBootTest(classes = MetadataPlatformApplication.class, properties = {
        "spring.datasource.primary.url=jdbc:h2:mem:collector_connector_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.primary.driver-class-name=org.h2.Driver",
        "spring.datasource.primary.username=sa",
        "spring.datasource.primary.password=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc
class CollectorConnectorListBootstrapTest {

    @Autowired
    private CollectorController collectorController;

    @Autowired
    private ConnectorController connectorController;

    @MockBean
    private DataSourceFeignClient dataSourceFeignClient;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(dataSourceFeignClient.pageDataSources(any()))
                .thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 20));
    }

    @Test
    @DisplayName("验证 CollectorController.list() 正常调用 MapStruct Convertor 而非触发 MyBatis BindingException")
    void testCollectorList() {
        MultiResult<CollectorVO> result = collectorController.list();
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("验证 ConnectorController.list() 正常调用 MapStruct Convertor 而非触发 MyBatis BindingException")
    void testConnectorList() {
        MultiResult<ConnectorVO> result = connectorController.list();
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("验证 Spring MVC 路由 /api/connectors 端点正常响应 200 与 YSS Result 包装")
    void testGetConnectorsEndpoint() throws Exception {
        mockMvc.perform(get("/api/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
