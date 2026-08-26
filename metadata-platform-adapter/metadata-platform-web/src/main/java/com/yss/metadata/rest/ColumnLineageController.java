package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.lineage.service.ColumnLineageAppService;
import com.yss.metadata.client.dto.cmd.ColumnLineageManualCmd;
import com.yss.metadata.client.dto.cmd.SqlParseReqDTO;
import com.yss.metadata.client.vo.ColumnLineageGraphVO;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.domain.lineage.parser.SqlLineageParser;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 字段级血缘控制器。
 * 提供字段血缘图谱检索、实时 SQL 语法解析、人工补录与删除操作。
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "column-lineage")
public class ColumnLineageController {

    private final ColumnLineageAppService columnLineageAppService;
    private final SqlLineageParser sqlLineageParser;

    /**
     * 实时 SQL AST 语法解析并提取表级与字段级血缘。
     */
    @PostMapping("/api/lineage/sql/parse")
    @ApiOperation(value = "SQL 实时血缘语法解析", notes = "输入 SQL DDL/DML，实时抽取源表、目标表及字段级转换表达式")
    public SingleResult<SqlLineageResult> parseSqlLineage(@Valid @RequestBody SqlParseReqDTO req) {
        return SingleResult.of(sqlLineageParser.parse(req.getSql(), req.getDialect()));
    }

    /**
     * 获取资产字段级血缘图谱。
     */
    @GetMapping("/api/assets/{id}/column-lineage")
    @ApiOperation(value = "资产字段级血缘图谱", notes = "支持指定 columnId 聚焦、上下游方向与递归深度过滤")
    public SingleResult<ColumnLineageGraphVO> getColumnLineage(
            @PathVariable("id") String id,
            @RequestParam(name = "columnId", required = false) String columnId,
            @RequestParam(name = "depth", required = false, defaultValue = "3") Integer depth,
            @RequestParam(name = "direction", required = false, defaultValue = "BOTH") String direction) {
        return SingleResult.of(columnLineageAppService.getColumnLineageGraph(id, columnId, depth, direction));
    }

    /**
     * 人工补录字段级血缘。
     */
    @PostMapping("/api/lineage/column/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "人工补录字段级血缘", notes = "支持字段级防环校验与版本乐观锁")
    public SingleResult<LineageEdgeVO> addManualColumnEdge(
            @Valid @RequestBody ColumnLineageManualCmd cmd,
            @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(columnLineageAppService.addManualColumnEdge(cmd, CurrentUser.resolve(userId)));
    }

    /**
     * 删除字段级血缘边。
     */
    @DeleteMapping("/api/lineage/column/{edgeId}")
    @ApiOperation(value = "删除字段级血缘边", notes = "按边 ID 删除手工或自动建立的字段血缘")
    public SingleResult<Boolean> deleteColumnEdge(
            @PathVariable("edgeId") String edgeId,
            @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        columnLineageAppService.deleteColumnEdge(edgeId, CurrentUser.resolve(userId));
        return SingleResult.of(true);
    }
}
