package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.domain.model.KeyReference;
import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.domain.model.KeyTaskReference;

import java.util.List;
import java.util.Optional;

public interface KeySecretGateway {
    List<KeySecret> pageKeys(int pageIndex, int pageSize, String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine, String currentUsername);
    long countKeys(String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine, String currentUsername);
    Optional<KeySecret> findById(Long id);
    Optional<KeySecret> findByName(String keyName);
    KeySecret save(KeySecret keySecret);
    KeySecret update(KeySecret keySecret);
    void deleteById(Long id);
    List<KeyReference> listReferences(Long keyId);
    List<KeyTaskReference> listTaskReferences(Long keyId);
    List<KeyPermission> listPermissions(Long keyId);
    KeyPermission savePermission(KeyPermission permission);
    void deletePermission(Long permissionId);
}
