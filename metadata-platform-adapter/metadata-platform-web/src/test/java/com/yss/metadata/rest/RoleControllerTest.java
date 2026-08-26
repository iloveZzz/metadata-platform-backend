package com.yss.metadata.rest;

import com.yss.metadata.application.rbac.service.convertor.RbacAppConvertor;
import com.yss.metadata.application.rbac.service.impl.RoleServiceImpl;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.rest.support.InMemoryAuditLogRepository;
import com.yss.metadata.rest.support.InMemoryRoleGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色管理 REST 契约测试（WU-06-04，冻结 OpenAPI rbac 段）。
 *
 * <p>覆盖：列表 200（含 refs）/创建 201（name 唯一 409）/删除 204（无绑定）/
 * 409 role.in_use（有绑定）/403 rbac.forbidden（非管理员）/422（name 为空）。</p>
 */
class RoleControllerTest {

    private InMemoryRoleGateway roleGateway;
    private InMemoryAuditLogRepository auditLogRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        roleGateway = new InMemoryRoleGateway();
        auditLogRepository = new InMemoryAuditLogRepository();
        RoleServiceImpl service = new RoleServiceImpl(roleGateway, auditLogRepository, org.mapstruct.factory.Mappers.getMapper(RbacAppConvertor.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(service))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/roles 管理员：200 角色列表（含 refs）")
    void listReturns200() throws Exception {
        roleGateway.seed(Role.builder().id("r-1").name("数据工程师").scope("交易域").build(), "交易域", "客户域");
        roleGateway.seed(Role.builder().id("r-2").name("只读角色").build());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("数据工程师"))
                .andExpect(jsonPath("$.data[0].refs").value(2))
                .andExpect(jsonPath("$.data[1].refs").value(0));
    }

    @Test
    @DisplayName("GET /api/roles 非管理员：403 rbac.forbidden")
    void listNonAdminReturns403() throws Exception {
        mockMvc.perform(get("/api/roles").header("X-User-Role", "user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("rbac.forbidden"));
    }

    @Test
    @DisplayName("POST /api/roles 创建：201 + 审计")
    void createReturns201() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "u-admin")
                        .content("{\"name\":\"数据治理专员\",\"scope\":\"交易/客户域\","
                                + "\"domains\":[\"交易域\",\"客户域\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("数据治理专员"))
                .andExpect(jsonPath("$.data.refs").value(2));

        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("rbac.role.create");
    }

    @Test
    @DisplayName("POST /api/roles name 重复：409 role.name_conflict")
    void createNameConflictReturns409() throws Exception {
        roleGateway.seed(Role.builder().id("r-1").name("平台管理员").build());

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"平台管理员\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("role.name_conflict"));
    }

    @Test
    @DisplayName("POST /api/roles 非管理员：403")
    void createNonAdminReturns403() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header("X-User-Role", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("rbac.forbidden"));
    }

    @Test
    @DisplayName("POST /api/roles name 为空：422 param.invalid")
    void createBlankNameReturns422() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("角色名称")));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} 无绑定：204 + 审计")
    void deleteReturns204() throws Exception {
        roleGateway.seed(Role.builder().id("r-1").name("只读角色").build());

        mockMvc.perform(delete("/api/roles/r-1").header("X-User-Id", "u-admin"))
                .andExpect(status().isNoContent());

        assertThat(roleGateway.all()).isEmpty();
        assertThat(auditLogRepository.entries()).hasSize(1);
        assertThat(auditLogRepository.entries().get(0).getAction()).isEqualTo("rbac.role.delete");
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} 有绑定：409 role.in_use")
    void deleteReferencedReturns409() throws Exception {
        roleGateway.seed(Role.builder().id("r-1").name("数据工程师").build(), "交易域");

        mockMvc.perform(delete("/api/roles/r-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("role.in_use"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1")));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} 非管理员：403")
    void deleteNonAdminReturns403() throws Exception {
        mockMvc.perform(delete("/api/roles/r-1").header("X-User-Role", "user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("rbac.forbidden"));
    }
}
