package com.yss.datamiddle.semantic.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 术语详情视图对象（冻结契约 TermDetail schema）。
 *
 * <p>synonymSet / attachments 由 SL-SLICE-03（同义词组）/ SL-SLICE-04（挂接）切片提供；
 * 本切片（SL-SLICE-01）返回 null / 空列表占位（数据架构 §11 表归属互斥）。</p>
 */
@Getter
@Setter
public class TermDetailVO implements Serializable {

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

    /** 关联同义词组 id（0..1） */
    private Long synonymSetId;

    /** 关联同义词组详情（SL-SLICE-03 后填充；本切片为 null） */
    private Object synonymSet;

    /** 挂接资产清单（含已解除；SL-SLICE-04 后填充；本切片为空列表） */
    private List<Object> attachments = new ArrayList<>();

    /** 乐观锁版本号 */
    private Integer version;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
