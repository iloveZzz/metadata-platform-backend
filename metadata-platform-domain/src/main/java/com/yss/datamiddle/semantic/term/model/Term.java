package com.yss.datamiddle.semantic.term.model;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 术语聚合根（TermAggregate：term + term_alias）。
 *
 * <p>领域规则均落位于本聚合（状态机 / 认证失效回退 SB-02 / 删除语义）；Application 仅做用例编排。</p>
 *
 * <p>状态机：draft → certified（certify）；draft / certified → deprecated（deprecate）；
 * deprecated 为终态（不可再认证）；certify / deprecate 幂等（重复执行返回当前状态）。</p>
 *
 * <p>Setter 仅供 Infrastructure 装配（PO → 领域模型 hydrate）与 id 赋值；业务变更一律走行为方法。</p>
 */
@Getter
@Setter
public class Term implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private List<String> aliases = new ArrayList<>();
    private String definition;
    private String description;
    private String owner;
    private TermStatus status;
    private String certifiedBy;
    private LocalDateTime certifiedAt;
    private String deprecatedBy;
    private LocalDateTime deprecatedAt;
    private Long synonymSetId;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 新建术语（初始草稿）。
     *
     * @param operator 当前操作者（yss-userinfo）
     */
    public static Term create(String name, List<String> aliases, String definition,
                              String description, String owner, String operator) {
        Term term = new Term();
        term.name = name;
        term.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        term.definition = definition;
        term.description = description;
        term.owner = owner;
        term.status = TermStatus.DRAFT;
        term.version = 0;
        term.createdBy = operator;
        LocalDateTime now = LocalDateTime.now();
        term.createdAt = now;
        term.updatedAt = now;
        return term;
    }

    /**
     * 内容变更（编辑）。已认证术语内容变更后认证失效退回草稿、需重新认证（SB-02 术语侧）。
     * 乐观锁版本自增（version + 1），由 Application 以条件 UPDATE（WHERE version=?）落库。
     */
    public void updateContent(String name, List<String> aliases, String definition,
                              String description, String owner) {
        this.name = name;
        this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        this.definition = definition;
        this.description = description;
        this.owner = owner;
        if (this.status == TermStatus.CERTIFIED) {
            this.status = TermStatus.DRAFT;
            this.certifiedBy = null;
            this.certifiedAt = null;
        }
        this.version = this.version + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 认证（draft → certified，幂等）。
     *
     * @throws StateConflictException 已弃用术语不可再认证
     */
    public void certify(String operator) {
        if (this.status == TermStatus.DEPRECATED) {
            throw new StateConflictException("已弃用术语不可再认证，请先更新内容（草稿）后认证");
        }
        if (this.status == TermStatus.CERTIFIED) {
            // 幂等：重复认证返回当前状态
            return;
        }
        this.status = TermStatus.CERTIFIED;
        this.certifiedBy = operator;
        this.certifiedAt = LocalDateTime.now();
        this.version = this.version + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 弃用（draft / certified → deprecated，幂等）。既有认证信息保留（历史可回溯，SB-09）。
     */
    public void deprecate(String operator) {
        if (this.status == TermStatus.DEPRECATED) {
            // 幂等：重复弃用返回当前状态
            return;
        }
        this.status = TermStatus.DEPRECATED;
        this.deprecatedBy = operator;
        this.deprecatedAt = LocalDateTime.now();
        this.version = this.version + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 删除前置校验：仅草稿可物理删除；非草稿抛 {@link StateConflictException}，提示改用弃用。
     * 引用检查（挂接 / 同义词组）由 Application 经 {@code TermReferenceCheckPort} 执行。
     */
    public void assertDeletable() {
        if (this.status != TermStatus.DRAFT) {
            throw new StateConflictException("仅草稿状态的术语可删除，请改用弃用");
        }
    }
}
