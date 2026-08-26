package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.InstallPackageDTO;
import com.yss.datasecurity.application.dto.ProjectPackageVO;
import com.yss.datasecurity.application.dto.StaticAlgorithmVO;
import com.yss.datasecurity.application.dto.StaticMaskTestDTO;
import com.yss.datasecurity.application.dto.StaticMaskTestResultVO;
import com.yss.datasecurity.application.service.StaticMaskingAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "静态脱敏与算法函数库管理")
@RestController
@RequestMapping("/api/v1/static-masking")
@RequiredArgsConstructor
@Validated
public class StaticMaskingController {

    private final StaticMaskingAppService staticMaskingAppService;

    @ApiOperation("查询静态脱敏算法函数库列表")
    @GetMapping("/algorithms")
    public MultiResult<StaticAlgorithmVO> listAlgorithms(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "algorithmType", required = false) String algorithmType) {
        List<StaticAlgorithmVO> list = staticMaskingAppService.listAlgorithms(keyword, algorithmType);
        return MultiResult.of(list);
    }

    @ApiOperation("查询项目脱敏算法包安装状态列表")
    @GetMapping("/packages")
    public MultiResult<ProjectPackageVO> listProjectPackages(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        List<ProjectPackageVO> list = staticMaskingAppService.listProjectPackages(keyword, status);
        return MultiResult.of(list);
    }

    @ApiOperation("为项目安装/更新脱敏算法包")
    @PostMapping("/packages/install")
    public SingleResult<Boolean> installPackage(@Valid @RequestBody InstallPackageDTO dto) {
        boolean success = staticMaskingAppService.installPackage(dto);
        return SingleResult.of(success);
    }

    @ApiOperation("在线测试静态脱敏算法运算")
    @PostMapping("/test-algorithm")
    public SingleResult<StaticMaskTestResultVO> testAlgorithm(@Valid @RequestBody StaticMaskTestDTO dto) {
        StaticMaskTestResultVO result = staticMaskingAppService.testAlgorithm(dto);
        return SingleResult.of(result);
    }
}
