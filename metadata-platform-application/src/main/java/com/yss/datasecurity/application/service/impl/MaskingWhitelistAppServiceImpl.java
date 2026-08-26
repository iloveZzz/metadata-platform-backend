package com.yss.datasecurity.application.service.impl;

import com.yss.datasecurity.application.dto.MaskingWhitelistCreateDTO;
import com.yss.datasecurity.application.dto.MaskingWhitelistVO;
import com.yss.datasecurity.application.service.MaskingWhitelistAppService;
import com.yss.datasecurity.domain.gateway.MaskingWhitelistGateway;
import com.yss.datasecurity.domain.gateway.SecurityAuditGateway;
import com.yss.datasecurity.domain.model.MaskingWhitelist;
import com.yss.datasecurity.domain.model.SecurityAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaskingWhitelistAppServiceImpl implements MaskingWhitelistAppService {

    private final MaskingWhitelistGateway whitelistGateway;
    private final SecurityAuditGateway auditGateway;

    @Override
    @Transactional
    public Long createWhitelist(MaskingWhitelistCreateDTO dto) {
        log.info("申请创建脱敏白名单: granteeType={}, granteeId={}, startTime={}, endTime={}",
                dto.getGranteeType(), dto.getGranteeId(), dto.getStartTime(), dto.getEndTime());

        String whitelistName = StringUtils.hasText(dto.getWhitelistName())
                ? dto.getWhitelistName()
                : (dto.getGranteeId() + " 免脱敏白名单");

        MaskingWhitelist entity = MaskingWhitelist.builder()
                .whitelistName(whitelistName)
                .granteeType(dto.getGranteeType())
                .granteeId(dto.getGranteeId())
                .categoryId(dto.getCategoryId())
                .ruleId(dto.getRuleId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status("ACTIVE")
                .reason(dto.getReason())
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Long id = whitelistGateway.save(entity);

        // 审计留痕
        auditGateway.save(SecurityAuditLog.builder()
                .actionType("WHITELIST_GRANT")
                .operator("admin")
                .clientIp("127.0.0.1")
                .targetResource("WHITELIST_" + id)
                .riskLevel("HIGH")
                .actionDetail(String.format("授予主体 [%s:%s] 免脱敏时效白名单，有效期至 %s",
                        dto.getGranteeType(), dto.getGranteeId(), dto.getEndTime()))
                .createdAt(LocalDateTime.now())
                .build());

        return id;
    }

    @Override
    @Transactional
    public void revokeWhitelist(Long id) {
        log.info("手动撤销脱敏白名单: id={}", id);
        MaskingWhitelist entity = whitelistGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("白名单记录不存在: " + id));

        entity.revoke();
        whitelistGateway.update(entity);

        // 审计留痕
        auditGateway.save(SecurityAuditLog.builder()
                .actionType("WHITELIST_REVOKE")
                .operator("admin")
                .clientIp("127.0.0.1")
                .targetResource("WHITELIST_" + id)
                .riskLevel("MEDIUM")
                .actionDetail(String.format("主动提前撤销主体 [%s:%s] 的免脱敏白名单",
                        entity.getGranteeType(), entity.getGranteeId()))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<MaskingWhitelistVO> pageWhitelists(int pageIndex, int pageSize, String status) {
        return whitelistGateway.findPage(pageIndex, pageSize, status).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countWhitelists(String status) {
        return whitelistGateway.countPage(status);
    }

    private MaskingWhitelistVO toVO(MaskingWhitelist entity) {
        return MaskingWhitelistVO.builder()
                .id(entity.getId())
                .whitelistName(StringUtils.hasText(entity.getWhitelistName()) ? entity.getWhitelistName() : (entity.getGranteeId() + " 免脱敏白名单"))
                .granteeType(entity.getGranteeType())
                .granteeId(entity.getGranteeId())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryId() != null ? "分类-" + entity.getCategoryId() : "全分类")
                .ruleId(entity.getRuleId())
                .ruleName(entity.getRuleId() != null ? "规则-" + entity.getRuleId() : "全规则")
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
