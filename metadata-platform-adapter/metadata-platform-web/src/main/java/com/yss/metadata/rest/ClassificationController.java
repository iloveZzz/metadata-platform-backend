package com.yss.metadata.rest;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.metadata.application.governance.service.ClassificationGovernanceService;
import com.yss.metadata.client.dto.cmd.ClassificationConfirmCmd;
import com.yss.metadata.client.dto.cmd.ClassRuleCmd;
import com.yss.metadata.client.dto.cmd.ClassRuleStatusCmd;
import com.yss.metadata.client.vo.ClassRuleVO;
import com.yss.metadata.client.vo.ClassificationOverviewVO;
import com.yss.metadata.client.vo.ClassificationVO;
import com.yss.metadata.client.vo.PropagateTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 分级分类控制器（冻结 OpenAPI classifications 段，WU-04-05）。
 *
 * <p>GET /api/classifications 概览（识别规则 + 识别结果）、POST 新增/修正规则（configure）、
 * PUT /{id}/status 规则启停（审计）、POST /{id}/confirm 确认/修正候选（幂等，可选 body 偏离登记）、
 * POST /{id}/propagate 触发传播（202 PropagateTask，同版本只跑一次幂等 + 审计）。
 * 当前用户上下文 seam（RBAC slice 06 替换）：operator 取请求头 X-User-Id
 * （缺省 default-user，见 {@link CurrentUser}）。</p>
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "classifications")
@RequestMapping("/api/classifications")
public class ClassificationController {

    private final ClassificationGovernanceService classificationGovernanceService;

    /**
     * 识别结果 / 规则列表（组合 VO，一次返回；0 候选空结构非错误）。
     */
    @GetMapping
    @ApiOperation(value = "识别结果 / 规则列表", notes = "组合 VO：rules + results；0 候选空结构非错误")
    public SingleResult<ClassificationOverviewVO> overview() {
        return SingleResult.of(classificationGovernanceService.getOverview());
    }

    /**
     * 新增 / 修正分类规则（configure）。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "新增/修正分类规则", notes = "冻结 spec 未声明 requestBody，body 以 ClassRuleCmd 为契约（前端经 options.data 透传）")
    public SingleResult<ClassRuleVO> create(@Valid @RequestBody ClassRuleCmd cmd,
                                            @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(classificationGovernanceService.createRule(cmd, CurrentUser.resolve(userId)));
    }

    /**
     * 规则启停（幂等；审计）。
     */
    @PutMapping("/{id}/status")
    @ApiOperation(value = "规则启停", notes = "幂等；审计 classify.rule.status")
    public SingleResult<ClassRuleVO> toggle(@PathVariable("id") String id,
                                            @Valid @RequestBody ClassRuleStatusCmd cmd,
                                            @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(classificationGovernanceService.toggleRule(id, cmd.getEnabled(), CurrentUser.resolve(userId)));
    }

    /**
     * 确认候选分类（幂等；correctedName 可选 = 修正语义，偏离登记）。
     */
    @PostMapping("/{id}/confirm")
    @ApiOperation(value = "确认/修正候选分类", notes = "确认幂等；correctedName 非空=修正（冻结 spec 未声明 body，偏离登记）")
    public SingleResult<ClassificationVO> confirm(@PathVariable("id") String id,
                                                  @RequestBody(required = false) ClassificationConfirmCmd cmd) {
        String correctedName = cmd == null ? null : cmd.getCorrectedName();
        return SingleResult.of(classificationGovernanceService.confirm(id, correctedName));
    }

    /**
     * 触发分类沿血缘传播（同版本只跑一次幂等；覆盖范围可核验；审计）。
     */
    @PostMapping("/{id}/propagate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "触发分类传播", notes = "202 PropagateTask；同 classification+version 只跑一次幂等；audit_log 审计")
    public SingleResult<PropagateTaskVO> propagate(@PathVariable("id") String id,
                                                   @RequestHeader(value = CurrentUser.HEADER, required = false) String userId) {
        return SingleResult.of(classificationGovernanceService.propagate(id, CurrentUser.resolve(userId)));
    }
}
