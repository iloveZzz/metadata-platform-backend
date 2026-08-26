package com.yss.metadata.rest;

import com.yss.metadata.application.integration.service.OpenLineageIngestionService;
import com.yss.metadata.client.dto.cmd.OpenLineageEventCmd;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * OpenLineage 事件接收控制器（冻结 OpenAPI /api/v1/lineage 段，WU-05-05）。
 *
 * <p>POST /api/v1/lineage：外部系统按 OpenLineage 标准协议推送 RunEvent 子集；
 * 校验失败 422，接收成功返回 202（事件已接收，无响应体）。</p>
 *
 * <p>事件接收为 202 语义（请求内同步执行，异步化随切片 05 重估）；接收端点
 * 只读展示（自身路径），无需持久化配置。</p>
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "integrations")
public class OpenLineageController {

    private final OpenLineageIngestionService openLineageIngestionService;

    /**
     * 接收 OpenLineage 事件（RunEvent 子集校验 422；成功 202 事件已接收）。
     */
    @PostMapping("/api/v1/lineage")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "OpenLineage 事件接收", notes = "标准协议 RunEvent 子集；校验失败返回 422；成功 202 事件已接收")
    public void receive(@Valid @RequestBody OpenLineageEventCmd cmd) {
        openLineageIngestionService.receive(cmd);
    }
}
