package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.integration.service.IntegrationService;
import com.yss.metadata.client.dto.cmd.IntegrationConfigCmd;
import com.yss.metadata.client.vo.IntegrationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 集成配置控制器（冻结 OpenAPI integrations 段，WU-05-05）。
 *
 * <p>GET /api/integrations 组合配置（Gravitino/DataHub/OpenLineage 端点与统计）、
 * PUT /api/integrations 保存（test=true 先测试 Gravitino 连接，失败 422 不保存）。
 * 响应统一 YSS Result 包装，错误体为 Error（code/message/severity/fieldErrors）。</p>
 *
 * <p>Web 层只做协议适配与响应包装，不做领域/VO 转换（由 Application 服务边界返回 VO）。
 * 当前用户上下文 seam（RBAC slice 06 替换）：operator 取请求头 X-User-Id
 * （缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
@Api(tags = "integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    /**
     * 集成配置查询（0 配置返回空结构，非错误）。
     */
    @GetMapping
    @ApiOperation(value = "集成配置查询", notes = "组合返回 Gravitino/DataHub/OpenLineage 端点与事件统计；未配置返回空结构而非错误")
    public SingleResult<IntegrationVO> getConfig() {
        return SingleResult.of(integrationService.getConfig());
    }

    /**
     * 更新集成配置（幂等 upsert 单例行 + 审计；test=true 连接测试失败返回 422 不保存）。
     *
     * <p>写路径为管理端面（slice 06）：非管理员 403 rbac.forbidden（浏览 GET 保持开放，
     * 浏览隐藏由前端菜单 adminOnly 门禁承载）。</p>
     */
    @PutMapping
    @ApiOperation(value = "更新集成配置", notes = "test=true 先测试 Gravitino 连接，失败返回 422 且不保存；凭据仅存加密引用；非管理员 403")
    public SingleResult<IntegrationVO> saveConfig(@Valid @RequestBody IntegrationConfigCmd cmd,
                                                  @RequestHeader(value = CurrentUser.HEADER, required = false) String userId,
                                                  @RequestHeader(value = RbacContext.ROLE_HEADER, required = false) String role) {
        RbacContext.requireAdmin(role);
        return SingleResult.of(integrationService.saveConfig(cmd, CurrentUser.resolve(userId)));
    }
}
