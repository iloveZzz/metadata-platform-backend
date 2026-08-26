package com.yss.datasecurity.rest;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.datasecurity.application.dto.KeyPermissionDTO;
import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeyReferenceVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretUpdateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;
import com.yss.datasecurity.application.dto.KeyTransferDTO;
import com.yss.datasecurity.application.service.KeySecretAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "安全密钥管理中心")
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Validated
public class KeySecretController {

    private final KeySecretAppService keySecretAppService;

    @ApiOperation("分页与多维筛选密钥列表")
    @GetMapping
    public PageResult<KeySecretVO> pageKeys(
            @RequestParam(name = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "keyType", required = false) String keyType,
            @RequestParam(name = "algorithm", required = false) String algorithm,
            @RequestParam(name = "genType", required = false) String genType,
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "isMine", required = false) Boolean isMine) {
        return keySecretAppService.pageKeys(pageIndex, pageSize, keyword, keyType, algorithm, genType, owner, isMine);
    }

    @ApiOperation("注册或自动生成安全密钥")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> createKey(@Valid @RequestBody KeySecretCreateDTO dto) {
        Long id = keySecretAppService.createKey(dto);
        return SingleResult.of(id);
    }

    @ApiOperation("编辑更新安全密钥")
    @PutMapping("/{id}")
    public SingleResult<Boolean> updateKey(@PathVariable("id") Long id, @Valid @RequestBody KeySecretUpdateDTO dto) {
        keySecretAppService.updateKey(id, dto);
        return SingleResult.of(true);
    }

    @ApiOperation("转交密钥负责人")
    @PostMapping("/{id}/transfer")
    public SingleResult<Boolean> transferOwner(@PathVariable("id") Long id, @Valid @RequestBody KeyTransferDTO dto) {
        keySecretAppService.transferOwner(id, dto.getNewOwner());
        return SingleResult.of(true);
    }

    @ApiOperation("删除安全密钥")
    @DeleteMapping("/{id}")
    public SingleResult<Boolean> deleteKey(@PathVariable("id") Long id) {
        keySecretAppService.deleteKey(id);
        return SingleResult.of(true);
    }

    @ApiOperation("查看密钥明文值 (高危操作，强制安全审计留痕)")
    @PostMapping("/{id}/reveal")
    public SingleResult<String> revealPlaintext(@PathVariable("id") Long id) {
        String plaintext = keySecretAppService.revealKeyPlaintext(id);
        return SingleResult.of(plaintext);
    }

    @ApiOperation("查询密钥脱敏规则引用列表")
    @GetMapping("/{id}/references")
    public MultiResult<KeyReferenceVO> getKeyReferences(@PathVariable("id") Long id) {
        List<KeyReferenceVO> references = keySecretAppService.getKeyReferences(id);
        return MultiResult.of(references);
    }

    @ApiOperation("查询密钥任务引用记录列表")
    @GetMapping("/{id}/task-references")
    public MultiResult<KeyTaskReferenceVO> listTaskReferences(@PathVariable("id") Long id) {
        List<KeyTaskReferenceVO> references = keySecretAppService.listTaskReferences(id);
        return MultiResult.of(references);
    }

    @ApiOperation("查询密钥授权列表")
    @GetMapping("/{id}/permissions")
    public MultiResult<KeyPermissionVO> listPermissions(@PathVariable("id") Long id) {
        List<KeyPermissionVO> permissions = keySecretAppService.listPermissions(id);
        return MultiResult.of(permissions);
    }

    @ApiOperation("授予密钥权限")
    @PostMapping("/{id}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SingleResult<Long> grantPermission(@PathVariable("id") Long id, @Valid @RequestBody KeyPermissionDTO dto) {
        Long permId = keySecretAppService.grantPermission(id, dto);
        return SingleResult.of(permId);
    }

    @ApiOperation("回收密钥权限")
    @DeleteMapping("/{id}/permissions/{permId}")
    public SingleResult<Boolean> revokePermission(@PathVariable("id") Long id, @PathVariable("permId") Long permId) {
        keySecretAppService.revokePermission(id, permId);
        return SingleResult.of(true);
    }
}
