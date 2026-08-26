package com.yss.datamiddle.aicontextlayer.repository.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.page.PageQuery;
import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.AuditLog;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.AuditLogGateway;
import com.yss.datamiddle.aicontextlayer.repository.AclAuditLogRepository;
import com.yss.datamiddle.aicontextlayer.infrastructure.convertor.AclAuditLogConvertor;
import com.yss.datamiddle.aicontextlayer.repository.entity.AuditLogPO;
import com.yss.datamiddle.aicontextlayer.repository.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 审计留痕端口实现（调用即写，不可变 SEC-06）。
 *
 * <p><b>不可变声明</b>：本实现仅提供 append-only 插入与只读查询，无 update / delete 路径
 * （SEC-06「无修改 / 删除路径」；数据库账号最小权限在部署时落实）。</p>
 */
@Repository("aclAuditLogGatewayImpl")
@RequiredArgsConstructor
public class AuditLogGatewayImpl implements AuditLogGateway {

    private final AclAuditLogRepository auditLogRepository;
    private final AclAuditLogConvertor auditLogConvertor;

    @Override
    @Transactional
    public String addAuditLog(AuditLog entity) {
        AuditLogPO po = auditLogConvertor.toPO(entity);
        auditLogRepository.insert(po);
        return po.getId();
    }

    @Override
    public Optional<AuditLog> getAuditLogById(String id) {
        return Optional.ofNullable(auditLogRepository.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<AuditLog> pageAuditLog(PageQuery query) {
        LambdaQueryWrapper<AuditLogPO> wrapper = Wrappers.lambdaQuery(AuditLogPO.class);
        
        IPage<AuditLogPO> result = auditLogRepository.selectPage(PageUtil.page(query), wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private AuditLog toDomain(AuditLogPO source) {
        return auditLogConvertor.toDomain(source);
    }
}
