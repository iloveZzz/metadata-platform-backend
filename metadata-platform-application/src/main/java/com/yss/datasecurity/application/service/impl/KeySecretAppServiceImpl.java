package com.yss.datasecurity.application.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datasecurity.application.convertor.KeySecretConvertor;
import com.yss.datasecurity.application.dto.KeyPermissionDTO;
import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeyReferenceVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretUpdateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;
import com.yss.datasecurity.application.service.KeySecretAppService;
import com.yss.datasecurity.domain.exception.DataSecurityException;
import com.yss.datasecurity.domain.gateway.KeySecretGateway;
import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.domain.model.KeyReference;
import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.domain.model.KeyTaskReference;
import com.yss.datasecurity.domain.service.KeyEnvelopeCryptoEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeySecretAppServiceImpl implements KeySecretAppService {

    private final KeySecretGateway keySecretGateway;
    private final KeyEnvelopeCryptoEngine cryptoEngine;
    private final KeySecretConvertor convertor;

    @Override
    public PageResult<KeySecretVO> pageKeys(int pageIndex, int pageSize, String keyword, String keyType, String algorithm, String genType, String owner, Boolean isMine) {
        String currentUsername = "admin";
        List<KeySecret> list = keySecretGateway.pageKeys(pageIndex, pageSize, keyword, keyType, algorithm, genType, owner, isMine, currentUsername);
        long total = keySecretGateway.countKeys(keyword, keyType, algorithm, genType, owner, isMine, currentUsername);
        List<KeySecretVO> voList = convertor.toVOList(list);
        return PageResult.of(voList, (int) total, pageSize, pageIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKey(KeySecretCreateDTO dto) {
        keySecretGateway.findByName(dto.getKeyName()).ifPresent(k -> {
            throw new DataSecurityException("KEY_NAME_DUPLICATE", "密钥名称已存在: " + dto.getKeyName());
        });

        String rawSecretKey;
        String publicKey = null;
        boolean isCustom = "CUSTOM".equalsIgnoreCase(dto.getGenType());

        if (isCustom) {
            String algo = dto.getAlgorithm() != null ? dto.getAlgorithm().toUpperCase() : "-";
            if ("RSA".equals(algo) || "SM2".equals(algo)) {
                if (dto.getPrivateKey() == null || dto.getPrivateKey().trim().isEmpty()) {
                    throw new DataSecurityException("KEY_VALUE_EMPTY", "自定义非对称密钥时私钥不能为空");
                }
                rawSecretKey = dto.getPrivateKey().trim();
                publicKey = (dto.getPublicKey() != null) ? dto.getPublicKey().trim() : null;
            } else {
                if (dto.getCustomKeyValue() == null || dto.getCustomKeyValue().trim().isEmpty()) {
                    throw new DataSecurityException("KEY_VALUE_EMPTY", "自定义密钥值不能为空");
                }
                rawSecretKey = dto.getCustomKeyValue().trim();
            }
        } else {
            KeyEnvelopeCryptoEngine.GeneratedKeyPair kp = cryptoEngine.generateKey(dto.getKeyType(), dto.getAlgorithm(), dto.getKeyLength());
            rawSecretKey = kp.getPrivateOrSecretKey();
            publicKey = kp.getPublicKey();
        }

        String encryptedSecret = cryptoEngine.encryptUnderMasterKey(rawSecretKey);

        KeySecret domain = convertor.toDomain(dto);
        domain.setKeyLength(dto.getKeyLength());
        domain.setGenType(dto.getGenType() != null ? dto.getGenType().toUpperCase() : "SYSTEM");
        domain.setOwnerOnly(Boolean.TRUE.equals(dto.getOwnerOnly()));
        domain.setEncryptedKeyValue(encryptedSecret);
        domain.setPublicKeyValue(publicKey);

        KeySecret saved = keySecretGateway.save(domain);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKey(Long id, KeySecretUpdateDTO dto) {
        KeySecret key = keySecretGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("KEY_NOT_FOUND", "密钥不存在: " + id));

        if (dto.getDescription() != null) {
            key.setDescription(dto.getDescription());
        }
        if (dto.getOwnerOnly() != null) {
            key.setOwnerOnly(dto.getOwnerOnly());
        }
        if (dto.getKeyType() != null) {
            key.setKeyType(dto.getKeyType());
        }
        if (dto.getAlgorithm() != null) {
            key.setAlgorithm(dto.getAlgorithm());
        }
        if (dto.getKeyLength() != null) {
            key.setKeyLength(dto.getKeyLength());
        }
        if (dto.getGenType() != null) {
            key.setGenType(dto.getGenType());
        }

        if ("CUSTOM".equalsIgnoreCase(dto.getGenType())) {
            String algo = key.getAlgorithm() != null ? key.getAlgorithm().toUpperCase() : "-";
            if ("RSA".equals(algo) || "SM2".equals(algo)) {
                if (dto.getPrivateKey() != null && !dto.getPrivateKey().trim().isEmpty()) {
                    key.setEncryptedKeyValue(cryptoEngine.encryptUnderMasterKey(dto.getPrivateKey().trim()));
                }
                if (dto.getPublicKey() != null && !dto.getPublicKey().trim().isEmpty()) {
                    key.setPublicKeyValue(dto.getPublicKey().trim());
                }
            } else if (dto.getCustomKeyValue() != null && !dto.getCustomKeyValue().trim().isEmpty()) {
                key.setEncryptedKeyValue(cryptoEngine.encryptUnderMasterKey(dto.getCustomKeyValue().trim()));
            }
        }

        keySecretGateway.update(key);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long id, String newOwner) {
        KeySecret key = keySecretGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("KEY_NOT_FOUND", "密钥不存在: " + id));
        key.setOwner(newOwner);
        keySecretGateway.update(key);
    }

    @Override
    public String revealKeyPlaintext(Long id) {
        KeySecret key = keySecretGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("KEY_NOT_FOUND", "密钥不存在: " + id));

        // 记录高危安全审计日志 (AUDIT_KEY_VIEW)
        log.warn("[HIGH_RISK_AUDIT] AUDIT_KEY_VIEW: User [admin] accessed plaintext key [{}] (ID: {}, Type: {}, Algorithm: {})",
                key.getKeyName(), key.getId(), key.getKeyType(), key.getAlgorithm());

        String decrypted = cryptoEngine.decryptUnderMasterKey(key.getEncryptedKeyValue());
        if (key.getPublicKeyValue() != null && !key.getPublicKeyValue().trim().isEmpty()) {
            return "【私钥 (Private Key)】:\n" + decrypted + "\n\n【公钥 (Public Key)】:\n" + key.getPublicKeyValue();
        }
        return decrypted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKey(Long id) {
        KeySecret key = keySecretGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("KEY_NOT_FOUND", "密钥不存在: " + id));

        List<KeyReference> refs = keySecretGateway.listReferences(id);
        List<KeyTaskReference> taskRefs = keySecretGateway.listTaskReferences(id);
        int totalRefs = (refs != null ? refs.size() : 0) + (taskRefs != null ? taskRefs.size() : 0);
        if (totalRefs > 0) {
            throw new DataSecurityException("KEY_IN_USE",
                    "密钥 [" + key.getKeyName() + "] 当前正被 " + totalRefs + " 个脱敏规则或数据任务引用，禁止删除！");
        }

        keySecretGateway.deleteById(id);
    }

    @Override
    public List<KeyReferenceVO> getKeyReferences(Long id) {
        List<KeyReference> refs = keySecretGateway.listReferences(id);
        return convertor.toReferenceVOList(refs);
    }

    @Override
    public List<KeyTaskReferenceVO> listTaskReferences(Long id) {
        List<KeyTaskReference> taskRefs = keySecretGateway.listTaskReferences(id);
        return convertor.toTaskReferenceVOList(taskRefs);
    }

    @Override
    public List<KeyPermissionVO> listPermissions(Long id) {
        List<KeyPermission> permissions = keySecretGateway.listPermissions(id);
        return convertor.toPermissionVOList(permissions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long grantPermission(Long id, KeyPermissionDTO dto) {
        keySecretGateway.findById(id)
                .orElseThrow(() -> new DataSecurityException("KEY_NOT_FOUND", "密钥不存在: " + id));

        KeyPermission permission = KeyPermission.builder()
                .keyId(id)
                .granteeType(dto.getGranteeType())
                .granteeId(dto.getGranteeId())
                .granteeName(dto.getGranteeName())
                .permissionType(dto.getPermissionType())
                .grantedBy("admin")
                .build();

        KeyPermission saved = keySecretGateway.savePermission(permission);
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePermission(Long id, Long permissionId) {
        keySecretGateway.deletePermission(permissionId);
    }
}
