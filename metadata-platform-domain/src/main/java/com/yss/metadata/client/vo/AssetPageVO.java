package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 资产搜索分页视图对象（应用层组装载体）。
 *
 * <p>Web 层以 yss-dto {@code PageResult} 包装输出：序列化形状为
 * {@code data}（数组）+ {@code totalCount/pageSize/pageIndex}
 * （见 freeze-record §4.2 修正记录；本 VO 的 list/total/page/size 仅作内部组装字段）。</p>
 */
@Getter
@Setter
public class AssetPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页资产列表 */
    private List<AssetVO> list;

    /** 命中总数 */
    private long total;

    /** 当前页码（从 1 起） */
    private int page;

    /** 每页大小 */
    private int size;
}
