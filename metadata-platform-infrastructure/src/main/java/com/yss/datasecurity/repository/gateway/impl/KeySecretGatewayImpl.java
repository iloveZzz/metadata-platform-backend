package com.yss.datasecurity.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.datasecurity.domain.gateway.KeySecretGateway;
import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.domain.model.KeyReference;
import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.domain.model.KeyTaskReference;
import com.yss.datasecurity.infrastructure.convertor.KeyPermissionPOConvertor;
import com.yss.datasecurity.infrastructure.convertor.KeySecretPOConvertor;
import com.yss.datasecurity.infrastructure.convertor.KeyTaskReferencePOConvertor;
import com.yss.datasecurity.repository.entity.KeyPermissionPO;
import com.yss.datasecurity.repository.entity.KeySecretPO;
import com.yss.datasecurity.repository.entity.KeyTaskReferencePO;
import com.yss.datasecurity.repository.mapper.KeyPermissionRepository;
import com.yss.datasecurity.repository.mapper.KeySecretRepository;
import com.yss.datasecurity.repository.mapper.KeyTaskReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class KeySecretGatewayImpl implements KeySecretGateway {

    private final KeySecretRepository repository;
    private final KeyPermissionRepository permissionRepository;
    private final KeyTaskReferenceRepository taskReferenceRepository;
    private final KeySecretPOConvertor convertor;
    private final KeyPermissionPOConvertor permissionConvertor;
    private final KeyTaskReferencePOConvertor taskReferenceConvertor;

    private LambdaQueryWrapper<KeySecretPO> buildQuery(String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine, String currentUsername) {
        LambdaQueryWrapper<KeySecretPO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(q -> q.like(KeySecretPO::getKeyName, keyword.trim())
                    .or()
                    .like(KeySecretPO::getDescription, keyword.trim()));
        }
        if (keyType != null && !keyType.trim().isEmpty()) {
            query.eq(KeySecretPO::getKeyType, keyType.trim());
        }
        if (algorithm != null && !algorithm.trim().isEmpty() && !"-".equals(algorithm.trim())) {
            query.eq(KeySecretPO::getAlgorithm, algorithm.trim());
        }
        if (genType != null && !genType.trim().isEmpty()) {
            query.eq(KeySecretPO::getGenType, genType.trim());
        }
        if (Boolean.TRUE.equals(isMine)) {
            String me = (currentUsername != null && !currentUsername.trim().isEmpty()) ? currentUsername.trim() : "admin";
            query.eq(KeySecretPO::getOwner, me);
        } else if (owner != null && !owner.trim().isEmpty()) {
            query.eq(KeySecretPO::getOwner, owner.trim());
        }
        query.orderByDesc(KeySecretPO::getCreatedAt);
        return query;
    }

    @Override
    public List<KeySecret> pageKeys(int pageIndex, int pageSize, String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine, String currentUsername) {
        Page<KeySecretPO> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<KeySecretPO> query = buildQuery(keyword, keyType, algorithm, genType, owner, isMine, currentUsername);
        Page<KeySecretPO> result = repository.selectPage(page, query);
        return convertor.toDomainList(result.getRecords());
    }

    @Override
    public long countKeys(String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine, String currentUsername) {
        LambdaQueryWrapper<KeySecretPO> query = buildQuery(keyword, keyType, algorithm, genType, owner, isMine, currentUsername);
        Long count = repository.selectCount(query);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<KeySecret> findById(Long id) {
        KeySecretPO po = repository.selectById(id);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public Optional<KeySecret> findByName(String keyName) {
        LambdaQueryWrapper<KeySecretPO> query = new LambdaQueryWrapper<KeySecretPO>()
                .eq(KeySecretPO::getKeyName, keyName);
        KeySecretPO po = repository.selectOne(query);
        return Optional.ofNullable(convertor.toDomain(po));
    }

    @Override
    public KeySecret save(KeySecret keySecret) {
        KeySecretPO po = convertor.toPO(keySecret);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        if (po.getCreatedBy() == null) {
            po.setCreatedBy("admin");
        }
        if (po.getUpdatedBy() == null) {
            po.setUpdatedBy("admin");
        }
        if (po.getOwnerOnly() == null) {
            po.setOwnerOnly(false);
        }
        if (po.getGenType() == null) {
            po.setGenType("SYSTEM");
        }
        repository.insert(po);
        return convertor.toDomain(po);
    }

    @Override
    public KeySecret update(KeySecret keySecret) {
        KeySecretPO po = convertor.toPO(keySecret);
        po.setUpdatedAt(LocalDateTime.now());
        po.setUpdatedBy("admin");
        repository.updateById(po);
        return convertor.toDomain(repository.selectById(po.getId()));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<KeyReference> listReferences(Long keyId) {
        KeySecretPO po = repository.selectById(keyId);
        if (po == null || po.getReferencedRulesCount() == null || po.getReferencedRulesCount() == 0) {
            return Collections.emptyList();
        }
        List<KeyReference> list = new ArrayList<>();
        list.add(KeyReference.builder()
                .ruleId(8001L)
                .ruleName("生产客户手机号 FPE-FF1 动态保留格式加密脱敏")
                .ruleType("DYNAMIC_MASK")
                .lastExecutedAt(LocalDateTime.now().minusHours(2))
                .build());
        return list;
    }

    @Override
    public List<KeyTaskReference> listTaskReferences(Long keyId) {
        LambdaQueryWrapper<KeyTaskReferencePO> query = new LambdaQueryWrapper<KeyTaskReferencePO>()
                .eq(KeyTaskReferencePO::getKeyId, keyId)
                .orderByDesc(KeyTaskReferencePO::getLastExecutedAt);
        List<KeyTaskReferencePO> pos = taskReferenceRepository.selectList(query);
        return taskReferenceConvertor.toDomainList(pos);
    }

    @Override
    public List<KeyPermission> listPermissions(Long keyId) {
        LambdaQueryWrapper<KeyPermissionPO> query = new LambdaQueryWrapper<KeyPermissionPO>()
                .eq(KeyPermissionPO::getKeyId, keyId)
                .orderByDesc(KeyPermissionPO::getGrantedAt);
        List<KeyPermissionPO> pos = permissionRepository.selectList(query);
        return permissionConvertor.toDomainList(pos);
    }

    @Override
    public KeyPermission savePermission(KeyPermission permission) {
        KeyPermissionPO po = permissionConvertor.toPO(permission);
        if (po.getGrantedAt() == null) {
            po.setGrantedAt(LocalDateTime.now());
        }
        if (po.getGrantedBy() == null) {
            po.setGrantedBy("admin");
        }
        permissionRepository.insert(po);
        return permissionConvertor.toDomain(po);
    }

    @Override
    public void deletePermission(Long permissionId) {
        permissionRepository.deleteById(permissionId);
    }
}
