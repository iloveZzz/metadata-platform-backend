package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.dto.MaskingWhitelistCreateDTO;
import com.yss.datasecurity.application.dto.MaskingWhitelistVO;
import com.yss.datasecurity.application.service.impl.MaskingWhitelistAppServiceImpl;
import com.yss.datasecurity.domain.gateway.MaskingWhitelistGateway;
import com.yss.datasecurity.domain.gateway.SecurityAuditGateway;
import com.yss.datasecurity.domain.model.MaskingWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaskingWhitelistAppServiceTest {

    @Mock
    private MaskingWhitelistGateway whitelistGateway;

    @Mock
    private SecurityAuditGateway auditGateway;

    @InjectMocks
    private MaskingWhitelistAppServiceImpl whitelistAppService;

    private MaskingWhitelist sampleWhitelist;

    @BeforeEach
    void setUp() {
        sampleWhitelist = MaskingWhitelist.builder()
                .id(1001L)
                .granteeType("USER")
                .granteeId("analyst_01")
                .categoryId(10L)
                .ruleId(20L)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status("ACTIVE")
                .reason("临时分析排障")
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateWhitelist() {
        when(whitelistGateway.save(any(MaskingWhitelist.class))).thenReturn(1001L);

        MaskingWhitelistCreateDTO dto = MaskingWhitelistCreateDTO.builder()
                .granteeType("USER")
                .granteeId("analyst_01")
                .categoryId(10L)
                .ruleId(20L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .reason("临时分析排障")
                .build();

        Long resultId = whitelistAppService.createWhitelist(dto);
        assertEquals(1001L, resultId);
        verify(whitelistGateway, times(1)).save(any(MaskingWhitelist.class));
        verify(auditGateway, times(1)).save(any());
    }

    @Test
    void testRevokeWhitelist() {
        when(whitelistGateway.findById(1001L)).thenReturn(Optional.of(sampleWhitelist));

        whitelistAppService.revokeWhitelist(1001L);
        assertEquals("REVOKED", sampleWhitelist.getStatus());
        verify(whitelistGateway, times(1)).update(sampleWhitelist);
        verify(auditGateway, times(1)).save(any());
    }

    @Test
    void testPageWhitelists() {
        when(whitelistGateway.findPage(1, 10, "ACTIVE")).thenReturn(Collections.singletonList(sampleWhitelist));
        when(whitelistGateway.countPage("ACTIVE")).thenReturn(1L);

        List<MaskingWhitelistVO> list = whitelistAppService.pageWhitelists(1, 10, "ACTIVE");
        long count = whitelistAppService.countWhitelists("ACTIVE");

        assertEquals(1, list.size());
        assertEquals("analyst_01", list.get(0).getGranteeId());
        assertEquals(1L, count);
    }
}
