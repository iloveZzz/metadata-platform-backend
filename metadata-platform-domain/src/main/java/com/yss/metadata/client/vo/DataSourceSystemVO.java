package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据源服务系统名录视图对象（WU-01-01：数据源服务业务系统名录）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceSystemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 系统编码 / 标识 (如 core-trading, marketing-crm) */
    private String code;

    /** 系统名称 (如 核心交易系统, 客户营销中台) */
    private String name;

    /** 显示标签 (如 核心交易系统 (Trading-Core)) */
    private String label;

    /** 业务分类/域 (如 核心交易域, 营销域, 风控域) */
    private String category;

    /** 系统描述 */
    private String description;
}
