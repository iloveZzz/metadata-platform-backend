package com.yss.datasecurity.domain.gateway;

import com.yss.datasecurity.domain.model.MaskingWhitelist;

import java.util.List;
import java.util.Optional;

public interface MaskingWhitelistGateway {
    Long save(MaskingWhitelist whitelist);
    void update(MaskingWhitelist whitelist);
    Optional<MaskingWhitelist> findById(Long id);
    List<MaskingWhitelist> findActiveByGrantee(String granteeType, String granteeId);
    List<MaskingWhitelist> findPage(int pageIndex, int pageSize, String status);
    long countPage(String status);
}
