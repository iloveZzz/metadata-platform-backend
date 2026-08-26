package com.yss.datamiddle.semantic.application.service;

import com.yss.datamiddle.semantic.application.model.SynonymSetCreateInput;
import com.yss.datamiddle.semantic.application.port.CurrentUserPort;
import com.yss.datamiddle.semantic.synonym.exception.SynonymConceptConflictException;
import com.yss.datamiddle.semantic.synonym.gateway.SynonymSetGateway;
import com.yss.datamiddle.semantic.synonym.model.SynonymSet;
import com.yss.datamiddle.semantic.term.exception.PermissionDeniedException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 同义词组应用服务（SL-003）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SynonymSetService {

    private final SynonymSetGateway synonymSetGateway;
    private final CurrentUserPort currentUserPort;

    public SynonymSet create(SynonymSetCreateInput input) {
        checkWritePermission();
        String operator = currentUserPort.userName();

        if (synonymSetGateway.findByName(input.getName()).isPresent()) {
            throw new SynonymConceptConflictException("组名已存在: " + input.getName());
        }
        if (synonymSetGateway.findByCanonical(input.getCanonical()).isPresent()) {
            throw new SynonymConceptConflictException("主词已存在于其他组: " + input.getCanonical());
        }

        SynonymSet set = SynonymSet.create(
                input.getName(),
                input.getCanonical(),
                input.getWords(),
                input.getTermId(),
                operator
        );
        return synonymSetGateway.save(set);
    }

    public void toggleStatus(Long id, boolean enabled) {
        checkWritePermission();
        String operator = currentUserPort.userName();
        SynonymSet s = synonymSetGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("同义词组不存在: " + id));
        s.toggleStatus(enabled, operator);
        synonymSetGateway.update(s);
    }

    public void delete(Long id) {
        checkWritePermission();
        SynonymSet s = synonymSetGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("同义词组不存在: " + id));
        if (Boolean.TRUE.equals(s.getEnabled())) {
            throw new StateConflictException("STATE_CONFLICT: 仅停用状态的同义词组可删除");
        }
        if (s.getTermId() != null) {
            throw new StateConflictException("REFERENCE_CONFLICT: 该同义词组已被术语关联，不可直接删除");
        }
        synonymSetGateway.delete(id);
    }

    public List<SynonymSet> list() {
        return synonymSetGateway.listAll();
    }

    public SynonymSet getById(Long id) {
        return synonymSetGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("同义词组不存在: " + id));
    }

    private void checkWritePermission() {
        if (!currentUserPort.isWritePermitted()) {
            throw new PermissionDeniedException("只读用户禁止执行写操作");
        }
    }
}
