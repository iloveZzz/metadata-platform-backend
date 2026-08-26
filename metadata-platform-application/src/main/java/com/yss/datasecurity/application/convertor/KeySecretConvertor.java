package com.yss.datasecurity.application.convertor;

import com.yss.datasecurity.application.dto.KeyPermissionVO;
import com.yss.datasecurity.application.dto.KeyReferenceVO;
import com.yss.datasecurity.application.dto.KeySecretCreateDTO;
import com.yss.datasecurity.application.dto.KeySecretVO;
import com.yss.datasecurity.application.dto.KeyTaskReferenceVO;
import com.yss.datasecurity.domain.model.KeyPermission;
import com.yss.datasecurity.domain.model.KeyReference;
import com.yss.datasecurity.domain.model.KeySecret;
import com.yss.datasecurity.domain.model.KeyTaskReference;
import com.yss.metadata.application.config.MapStructAppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructAppConfig.class)
public interface KeySecretConvertor {

    KeySecretVO toVO(KeySecret domain);
    List<KeySecretVO> toVOList(List<KeySecret> domainList);

    KeyReferenceVO toReferenceVO(KeyReference domain);
    List<KeyReferenceVO> toReferenceVOList(List<KeyReference> domainList);

    KeyPermissionVO toPermissionVO(KeyPermission domain);
    List<KeyPermissionVO> toPermissionVOList(List<KeyPermission> domainList);

    KeyTaskReferenceVO toTaskReferenceVO(KeyTaskReference domain);
    List<KeyTaskReferenceVO> toTaskReferenceVOList(List<KeyTaskReference> domainList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encryptedKeyValue", ignore = true)
    @Mapping(target = "publicKeyValue", ignore = true)
    @Mapping(target = "owner", constant = "admin")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "referencedRulesCount", constant = "0")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    KeySecret toDomain(KeySecretCreateDTO dto);
}
