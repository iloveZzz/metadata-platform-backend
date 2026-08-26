package com.yss.smartdiscovery.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.smartdiscovery.application.dto.SandboxResultDTO;
import com.yss.smartdiscovery.application.dto.TagDTO;
import com.yss.smartdiscovery.application.dto.TagRuleDTO;
import com.yss.smartdiscovery.application.service.TagTaxonomyAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class TagTaxonomyControllerTest {

    private TagTaxonomyAppService appService;
    private TagTaxonomyController controller;

    @BeforeEach
    void setUp() {
        appService = Mockito.mock(TagTaxonomyAppService.class);
        controller = new TagTaxonomyController(appService);
    }

    @Test
    @DisplayName("GET /tags - 查询标签列表")
    void testListTags() {
        TagDTO tag = TagDTO.builder().id("TAG-01").name("测试标签").code("SEC_TEST").build();
        Mockito.when(appService.listTags("SECURITY")).thenReturn(Collections.singletonList(tag));

        MultiResult<TagDTO> res = controller.listTags("SECURITY");
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData()).hasSize(1);
        assertThat(res.getData().get(0).getName()).isEqualTo("测试标签");
    }

    @Test
    @DisplayName("POST /tags - 创建标签定义")
    void testCreateTag() {
        TagDTO input = TagDTO.builder().name("新标签").code("NEW_TAG").categoryCode("DOMAIN").build();
        TagDTO created = TagDTO.builder().id("TAG-99").name("新标签").code("NEW_TAG").build();
        Mockito.when(appService.createTag(any())).thenReturn(created);

        SingleResult<TagDTO> res = controller.createTag(input);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().getId()).isEqualTo("TAG-99");
    }

    @Test
    @DisplayName("POST /tags/sandbox-test - 在线沙箱规则测试")
    void testSandboxTest() {
        SandboxResultDTO resultDTO = SandboxResultDTO.builder()
                .matchedTagName("L4 核心敏感数据")
                .confidence(0.98)
                .l1RegexHit(true)
                .build();
        Mockito.when(appService.testSandboxRule("cust_id_card", "身份证号")).thenReturn(resultDTO);

        Map<String, String> request = new HashMap<>();
        request.put("fieldName", "cust_id_card");
        request.put("fieldComment", "身份证号");

        SingleResult<SandboxResultDTO> res = controller.testSandboxRule(request);
        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getData().getL1RegexHit()).isTrue();
        assertThat(res.getData().getConfidence()).isEqualTo(0.98);
    }
}
