package com.yss.metadata.domain.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志分页结果（slice 06 查询；只读不可变）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页记录（time DESC） */
    @Builder.Default
    private List<AuditLogEntry> items = new ArrayList<>();

    /** 总记录数 */
    private long total;

    /** 页码（从 1 起） */
    private int pageIndex;

    /** 每页大小 */
    private int pageSize;
}
