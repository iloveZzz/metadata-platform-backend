package com.yss.datamiddle.aicontextlayer.mcpserver;

import com.yss.datamiddle.aicontextlayer.application.service.ClassificationToolApplicationService;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.ReadOnlyToolRegistry;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ClassificationQueryResult;
import com.yss.datamiddle.aicontextlayer.mcpserver.interceptor.MethodInterceptor;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.ClassificationQueryToolHandler;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolDispatcher;
import com.yss.datamiddle.aicontextlayer.mcpserver.tool.McpToolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class Slice04SecurityAssertionsTest {

    private MetadataPlatformGateway metadataGateway;
    private ClassificationToolApplicationService classificationService;
    private McpToolDispatcher dispatcher;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AGENT_ID = "agent-001";

    @BeforeEach
    void setUp() {
        metadataGateway = Mockito.mock(MetadataPlatformGateway.class);
        classificationService = new ClassificationToolApplicationService(metadataGateway);

        ReadOnlyToolRegistry registry = new ReadOnlyToolRegistry();
        MethodInterceptor interceptor = new MethodInterceptor(registry);

        List<McpToolHandler> handlers = Arrays.asList(
                new ClassificationQueryToolHandler(classificationService)
        );
        dispatcher = new McpToolDispatcher(interceptor, handlers);
    }

    @Test
    @DisplayName("断言 7: classification_query 分类溯源为 classificationId 且不可篡改")
    void classificationProvenanceCorrect() {
        when(metadataGateway.getClassifications(anyString())).thenReturn("[]");

        Map<String, Object> args = new HashMap<>();
        args.put("page", 1);
        args.put("size", 20);

        ClassificationQueryResult result = (ClassificationQueryResult) dispatcher.dispatch(AGENT_ID, BASE_URL, "classification_query", args);
        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertNotNull(result.getItems().get(0).getProvenance().getClassificationId());
    }
}
