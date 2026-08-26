package com.yss.datamiddle.semantic.audit;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计记录值对象（audit_log，不可变只追加，与业务写操作同事务）。
 */
public class AuditLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String operator;
    private String action;
    private String objectType;
    private Long objectId;
    private String note;
    private String result;
    private LocalDateTime createdAt;

    private AuditLogEntry() {
    }

    public static AuditLogEntry of(String operator, AuditAction action, String objectType,
                                   Long objectId, String note, AuditResult result) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.operator = operator;
        entry.action = action.getCode();
        entry.objectType = objectType;
        entry.objectId = objectId;
        entry.note = note;
        entry.result = result.getCode();
        entry.createdAt = LocalDateTime.now();
        return entry;
    }

    public Long getId() {
        return id;
    }

    public String getOperator() {
        return operator;
    }

    public String getAction() {
        return action;
    }

    public String getObjectType() {
        return objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public String getNote() {
        return note;
    }

    public String getResult() {
        return result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
