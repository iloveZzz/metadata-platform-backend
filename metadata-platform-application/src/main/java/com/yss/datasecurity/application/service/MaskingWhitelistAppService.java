package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.dto.MaskingWhitelistCreateDTO;
import com.yss.datasecurity.application.dto.MaskingWhitelistVO;

import java.util.List;

public interface MaskingWhitelistAppService {
    Long createWhitelist(MaskingWhitelistCreateDTO dto);
    void revokeWhitelist(Long id);
    List<MaskingWhitelistVO> pageWhitelists(int pageIndex, int pageSize, String status);
    long countWhitelists(String status);
}
