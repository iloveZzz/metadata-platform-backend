package com.yss.datamiddle.semantic.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语视图对象（冻结契约 Term schema）。
 */
@Getter
@Setter
public class TermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private List<String> aliases;

    private String definition;

    private String description;

    /** 负责人（SB-01：语义对象自带） */
    private String owner;

    /** 状态：draft / certified / deprecated */
    private String status;

    private String certifiedBy;

    private LocalDateTime certifiedAt;

    private String deprecatedBy;

    private LocalDateTime deprecatedAt;

    /** 关联同义词组（0..1，SL-SLICE-03 写入） */
    private Long synonymSetId;

    /** 乐观锁版本号 */
    private Integer version;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
