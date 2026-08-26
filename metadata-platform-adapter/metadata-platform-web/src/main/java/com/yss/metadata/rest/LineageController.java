package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.lineage.service.LineageActionService;
import com.yss.metadata.application.lineage.service.LineageQueryService;
import com.yss.metadata.client.dto.cmd.LineageManualCmd;
import com.yss.metadata.client.vo.LineageEdgeVO;
import com.yss.metadata.client.vo.LineageGraphVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * 血缘控制器（冻结 OpenAPI lineage 段，WU-03-05）。
 *
 * <p>GET /api/assets/{id}/lineage 血缘图谱（confidence 筛选，空血缘空结构）、
 * POST /api/lineage/manual 人工补录（成环 CYCLE 409 / 图版本 token CONFLICT 409 /
 * 参数校验 422）。当前用户上下文 seam（RBAC slice 06 替换）：operator 取请求头
 * X-User-Id（缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "lineage")
public class LineageController {

    private final LineageQueryService lineageQueryService;
    private final LineageActionService lineageActionService;

    /**
     * 血缘图谱（边带 confidence；空血缘可表达；graphVersionToken 供补录乐观锁；支持 withQuality 质量热力染色）。
     */
    @GetMapping("/api/assets/{id}/lineage")
    @ApiOperation(value = "血缘图谱", notes = "confidence 筛选；withQuality 是否叠加质量健康分与断流连线；空血缘空结构非错误")
    public SingleResult<LineageGraphVO> graph(@PathVariable("id") String id,
                                              @RequestParam(name = "confidence", defaultValue = "all") String confidence,
                                              @RequestParam(name = "withQuality", required = false, defaultValue = "false") Boolean withQuality) {
        return SingleResult.of(lineageQueryService.getGraph(id, confidence));
    }


    /**
     * 人工补录血缘（环检测 CYCLE；图版本 token CONFLICT）。
     */
    @PostMapping("/api/lineage/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "人工补录血缘", notes = "成环返回 409 CYCLE（定位冲突边）；token 不匹配返回 409 CONFLICT；重复边幂等返回既有边")
    public SingleResult<LineageEdgeVO> manual(@Valid @RequestBody LineageManualCmd cmd,
                                              @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(lineageActionService.addManualEdge(cmd, CurrentUser.resolve(userId)));
    }
}
