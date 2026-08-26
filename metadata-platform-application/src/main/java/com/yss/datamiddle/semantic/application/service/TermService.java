package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.TermCertifyInput;
import com.yss.datamiddle.semantic.application.model.TermCreateInput;
import com.yss.datamiddle.semantic.application.model.TermUpdateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.audit.AuditAction;
import com.yss.datamiddle.semantic.audit.AuditLogEntry;
import com.yss.datamiddle.semantic.audit.AuditLogGateway;
import com.yss.datamiddle.semantic.audit.AuditResult;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.ReferenceConflictException;
import com.yss.datamiddle.semantic.term.exception.TermNameDuplicateException;
import com.yss.datamiddle.semantic.term.exception.TermNotFoundException;
import com.yss.datamiddle.semantic.term.exception.VersionConflictException;
import com.yss.datamiddle.semantic.term.gateway.TermGateway;
import com.yss.datamiddle.semantic.term.gateway.TermReferenceCheckPort;
import com.yss.datamiddle.semantic.term.model.CertifyAction;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 术语写用例编排（单聚合事务：term + term_alias + 审计同事务）。
 *
 * <p>领域规则（状态机 / 认证失效回退 / 删除语义）在 {@link Term} 聚合内；本服务只做
 * 用例编排、跨聚合协调（名称唯一校验 / 删除引用检查）与事务边界。</p>
 */
@Service
@RequiredArgsConstructor
public class TermService {

    private static final String OBJECT_TYPE_TERM = "term";

    private final TermGateway termGateway;
    private final AuditLogGateway auditLogGateway;
    private final CurrentUserPort currentUserPort;
    private final TermReferenceCheckPort termReferenceCheckPort;

    /**
     * 新建术语（保存为草稿；名称唯一，重复返回 422 TERM_NAME_DUPLICATE）。
     */
    @Transactional
    public Term createTerm(TermCreateInput input) {
        String operator = requireWritePermission(null, AuditAction.CREATE, null);
        if (termGateway.existsByName(input.getName(), null)) {
            throw new TermNameDuplicateException(input.getName());
        }
        Term term = Term.create(input.getName(), input.getAliases(), input.getDefinition(),
                input.getDescription(), input.getOwner(), operator);
        termGateway.save(term);
        auditLogGateway.append(AuditLogEntry.of(operator, AuditAction.CREATE,
                OBJECT_TYPE_TERM, term.getId(), null, AuditResult.SUCCESS));
        return term;
    }

    /**
     * 更新术语（乐观锁 version；已认证内容变更后认证失效退回草稿需重新认证，SB-02 术语侧）。
     *
     * @throws VersionConflictException 版本过期（409，携带最新对象，拒绝覆盖他人修改）
     */
    @Transactional
    public Term updateTerm(Long id, TermUpdateInput input) {
        String operator = requireWritePermission(id, AuditAction.UPDATE, null);
        Term term = findOrThrow(id);
        if (input.getVersion() == null || !term.getVersion().equals(input.getVersion())) {
            throw new VersionConflictException("版本过期，已被他人修改，请刷新后重试", term);
        }
        if (termGateway.existsByName(input.getName(), id)) {
            throw new TermNameDuplicateException(input.getName());
        }
        term.updateContent(input.getName(), input.getAliases(), input.getDefinition(),
                input.getDescription(), input.getOwner());
        if (!termGateway.updateWithVersion(term, input.getVersion())) {
            throw new VersionConflictException("版本过期，已被他人修改，请刷新后重试", findOrThrow(id));
        }
        auditLogGateway.append(AuditLogEntry.of(operator, AuditAction.UPDATE,
                OBJECT_TYPE_TERM, id, null, AuditResult.SUCCESS));
        return term;
    }

    /**
     * 认证 / 弃用（幂等，重复执行返回当前状态并写审计；已弃用不可再认证 409 STATE_CONFLICT）。
     */
    @Transactional
    public Term certifyTerm(Long id, TermCertifyInput input) {
        AuditAction auditAction = toAuditAction(input.getAction());
        String operator = requireWritePermission(id, auditAction, input.getNote());
        Term term = findOrThrow(id);
        int oldVersion = term.getVersion();
        applyCertifyAction(term, input.getAction(), operator);
        if (term.getVersion().equals(oldVersion)) {
            // 幂等：状态未变化，无需更新，但保留审计痕迹
            auditLogGateway.append(AuditLogEntry.of(operator, auditAction,
                    OBJECT_TYPE_TERM, id, input.getNote(), AuditResult.SUCCESS));
            return term;
        }
        if (!termGateway.updateWithVersion(term, oldVersion)) {
            Term latest = findOrThrow(id);
            if (stateMatchesAction(latest, input.getAction())) {
                // 并发操作已达成目标状态：幂等返回当前状态 + 审计
                auditLogGateway.append(AuditLogEntry.of(operator, auditAction,
                        OBJECT_TYPE_TERM, id, input.getNote(), AuditResult.SUCCESS));
                return latest;
            }
            throw new VersionConflictException("版本过期，已被他人修改，请刷新后重试", latest);
        }
        auditLogGateway.append(AuditLogEntry.of(operator, auditAction,
                OBJECT_TYPE_TERM, id, input.getNote(), AuditResult.SUCCESS));
        return term;
    }

    /**
     * 删除术语（仅草稿且未被挂接 / 未被同义词组关联可物理删除；否则 409 提示改用弃用）。
     */
    @Transactional
    public void deleteTerm(Long id) {
        String operator = requireWritePermission(id, AuditAction.DELETE, null);
        Term term = findOrThrow(id);
        term.assertDeletable();
        if (termReferenceCheckPort.isReferenced(id)) {
            throw new ReferenceConflictException("术语已被挂接或关联同义词组，不可删除，请改用弃用");
        }
        termGateway.delete(term);
        auditLogGateway.append(AuditLogEntry.of(operator, AuditAction.DELETE,
                OBJECT_TYPE_TERM, id, null, AuditResult.SUCCESS));
    }

    private Term findOrThrow(Long id) {
        return termGateway.findById(id).orElseThrow(() -> new TermNotFoundException(id));
    }

    /**
     * 写权限兜底（SB-08 / CT-10 可测 seam）：只读用户直调写接口 → 记录 DENIED 审计后 403。
     */
    private String requireWritePermission(Long objectId, AuditAction action, String note) {
        if (!currentUserPort.isWritePermitted()) {
            auditLogGateway.appendDenied(AuditLogEntry.of(currentUserPort.userName(), action,
                    OBJECT_TYPE_TERM, objectId, note, AuditResult.DENIED));
            throw new PermissionDeniedException("当前用户为只读角色，无写操作权限");
        }
        return currentUserPort.userName();
    }

    private void applyCertifyAction(Term term, CertifyAction action, String operator) {
        if (action == CertifyAction.CERTIFY) {
            term.certify(operator);
        } else {
            term.deprecate(operator);
        }
    }

    private boolean stateMatchesAction(Term term, CertifyAction action) {
        if (action == CertifyAction.CERTIFY) {
            return term.getStatus() == TermStatus.CERTIFIED;
        }
        return term.getStatus() == TermStatus.DEPRECATED;
    }

    private AuditAction toAuditAction(CertifyAction action) {
        return action == CertifyAction.CERTIFY ? AuditAction.CERTIFY : AuditAction.DEPRECATE;
    }
}
