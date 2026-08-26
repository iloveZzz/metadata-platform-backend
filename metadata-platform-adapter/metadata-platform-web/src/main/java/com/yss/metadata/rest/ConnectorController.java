package com.yss.metadata.rest;

import com.yss.metadata.application.connector.service.ConnectorAppService;
import com.yss.metadata.client.dto.cmd.ConnectorAddCmd;
import com.yss.metadata.client.dto.cmd.ConnectorUpdateCmd;
import com.yss.metadata.client.vo.ConnectTestVO;
import com.yss.metadata.client.vo.ConnectorVO;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 连接器管理控制器（冻结 OpenAPI connectors 段，WU-01-01）。
 *
 * <p>GET/POST /api/connectors、PUT/DELETE /api/connectors/{id}、
 * POST /api/connectors/{id}/test；响应统一 YSS Result 包装，
 * 错误体为 Error（code/message/severity/fieldErrors）。</p>
 */
@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
@Api(tags = "connectors")
public class ConnectorController {

    private final ConnectorAppService connectorAppService;

    /**
     * 连接器列表。
     */
    @GetMapping
    @ApiOperation(value = "连接器列表", notes = "返回 YSS Result 包装的连接器列表")
    public MultiResult<ConnectorVO> list() {
        return MultiResult.of(connectorAppService.list());
    }

    /**
     * 数据源类型统计指标（已创建实例数、已采集资产数）。
     */
    @GetMapping("/stats")
    @ApiOperation(value = "数据源类型统计", notes = "返回各数据源类型的实例计数与采集资产统计")
    public MultiResult<com.yss.metadata.client.vo.ConnectorTypeStatsVO> stats() {
        return MultiResult.of(connectorAppService.getTypeStats());
    }

    /**
     * 数据源服务系统名录。
     */
    @GetMapping("/systems")
    @ApiOperation(value = "数据源服务系统名录", notes = "返回数据源服务维护的业务系统名录列表，用于采集任务来源系统选择")
    public MultiResult<com.yss.metadata.client.vo.DataSourceSystemVO> getSystemCatalog() {
        return MultiResult.of(connectorAppService.getSystemCatalog());
    }

    /**
     * 获取指定数据源下的 Database / Catalog 列表。
     */
    @GetMapping("/{id}/databases")
    @ApiOperation(value = "数据源 Database 列表", notes = "返回指定数据源下的 Database / Catalog / Schema 列表，由数据源微服务元数据客户端提供")
    public MultiResult<String> getDatabases(@PathVariable("id") String id) {
        return MultiResult.of(connectorAppService.listDatabases(id));
    }

    /**
     * 新增连接器（create-draft / configure）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "新增连接器", notes = "name 唯一，重复返回 409；参数校验失败返回 422")
    public SingleResult<ConnectorVO> create(@Valid @RequestBody ConnectorAddCmd cmd) {
        return SingleResult.of(connectorAppService.create(cmd));
    }

    /**
     * 更新连接器配置。
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "更新连接器配置", notes = "连接器不存在返回 404；配置变更后状态重置为草稿")
    public SingleResult<ConnectorVO> update(@PathVariable("id") String id,
                                            @Valid @RequestBody ConnectorUpdateCmd cmd) {
        return SingleResult.of(connectorAppService.update(id, cmd));
    }

    /**
     * 删除连接器（不可逆，需确认）。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "删除连接器", notes = "连接器不存在返回 404")
    public void delete(@PathVariable("id") String id) {
        connectorAppService.delete(id);
    }

    /**
     * 测试连接（错误分类：网络/凭据/方言）。
     */
    @PostMapping("/{id}/test")
    @ApiOperation(value = "测试连接", notes = "失败返回 422，错误体含 code/message/severity/fieldErrors 与分类错误码")
    public SingleResult<ConnectTestVO> testConnection(@PathVariable("id") String id) {
        ConnectTestResult result = connectorAppService.testConnection(id);
        return SingleResult.of(ConnectTestVO.of(result.isConnected(), result.getMessage()));
    }
}
