package com.yss.metadata.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.rbac.service.RoleService;
import com.yss.metadata.client.dto.cmd.RoleCmd;
import com.yss.metadata.client.vo.RoleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 角色管理控制器（冻结 OpenAPI rbac 段，WU-06-04）。
 *
 * <p>GET /api/roles 列表（refs=role_domain 绑定数）、POST 创建（name 唯一 409）、
 * DELETE /api/roles/{id}（被引用 409）。管理端面：全部端点管理员门禁（403
 * rbac.forbidden，非管理员经 RbacContext 拒绝）。</p>
 *
 * <p>当前用户上下文 seam（RBAC slice 06）：operator 取请求头 X-User-Id（缺省
 * default-user，见 {@link CurrentUser}）；管理员判定取 X-User-Role（缺省 admin，
 * 见 {@link RbacContext}）。</p>
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Api(tags = "rbac")
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色列表（含 refs）。
     */
    @GetMapping
    @ApiOperation(value = "角色列表", notes = "返回含 refs（数据域绑定数）的角色列表；非管理员 403")
    public MultiResult<RoleVO> list(@RequestHeader(value = RbacContext.ROLE_HEADER, required = false) String role) {
        RbacContext.requireAdmin(role);
        return MultiResult.of(roleService.list());
    }

    /**
     * 创建角色（name 唯一冲突 409；绑定数据域）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "创建角色", notes = "name 唯一，重复 409；非管理员 403")
    public SingleResult<RoleVO> create(@Valid @RequestBody RoleCmd cmd,
                                       @RequestHeader(value = RbacContext.ROLE_HEADER, required = false) String role,
                                       @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        RbacContext.requireAdmin(role);
        return SingleResult.of(roleService.create(cmd, CurrentUser.resolve(userId)));
    }

    /**
     * 删除角色（被数据域绑定引用返回 409）。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "删除角色", notes = "存在数据域绑定返回 409 role.in_use；非管理员 403")
    public void delete(@PathVariable("id") String id,
                       @RequestHeader(value = RbacContext.ROLE_HEADER, required = false) String role,
                       @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        RbacContext.requireAdmin(role);
        roleService.delete(id, CurrentUser.resolve(userId));
    }
}
