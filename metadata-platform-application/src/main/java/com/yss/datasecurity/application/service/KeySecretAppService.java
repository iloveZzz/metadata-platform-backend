package com.yss.datasecurity.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.dto.KeyPermissionDTO;
import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeyReferenceVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretUpdateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;

import java.util.List;

public interface KeySecretAppService {
    PageResult<KeySecretVO> pageKeys(int pageIndex, int pageSize, String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine);
    Long createKey(KeySecretCreateDTO dto);
    void updateKey(Long id, KeySecretUpdateDTO dto);
    void transferOwner(Long id, String newOwner);
    String revealKeyPlaintext(Long id);
    void deleteKey(Long id);
    List<KeyReferenceVO> getKeyReferences(Long id);
    List<KeyTaskReferenceVO> listTaskReferences(Long id);
    List<KeyPermissionVO> listPermissions(Long id);
    Long grantPermission(Long id, KeyPermissionDTO dto);
    void revokePermission(Long id, Long permissionId);
}
