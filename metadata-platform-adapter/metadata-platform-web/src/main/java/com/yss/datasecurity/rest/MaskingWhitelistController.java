package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.MaskingWhitelistCreateDTO;
import com.yss.datasecurity.application.dto.MaskingWhitelistVO;
import com.yss.datasecurity.application.service.MaskingWhitelistAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 动态脱敏时效白名单接口控制器
 */
@Validated
@RestController
@RequestMapping("/api/v1/masking-whitelists")
@RequiredArgsConstructor
public class MaskingWhitelistController {

    private final MaskingWhitelistAppService whitelistAppService;

    /**
     * 分页查询脱敏白名单
     */
    @GetMapping
    public PageResult<MaskingWhitelistVO> pageMaskingWhitelists(
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        List<MaskingWhitelistVO> list = whitelistAppService.pageWhitelists(pageIndex, pageSize, status);
        long total = whitelistAppService.countWhitelists(status);
        return PageResult.of(list, total, pageSize, pageIndex);
    }

    /**
     * 申请创建临时明文脱敏白名单
     */
    @PostMapping
    public SingleResult<Long> createMaskingWhitelist(@Valid @RequestBody MaskingWhitelistCreateDTO dto) {
        Long id = whitelistAppService.createWhitelist(dto);
        return SingleResult.of(id);
    }

    /**
     * 手动撤销/提前失效白名单
     */
    @PostMapping("/{id}/revoke")
    public SingleResult<Boolean> revokeMaskingWhitelist(@PathVariable Long id) {
        whitelistAppService.revokeWhitelist(id);
        return SingleResult.of(true);
    }
}
