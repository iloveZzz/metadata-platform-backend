package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 编辑标签命令（冻结 OpenAPI PUT /api/assets/{id}/tags；覆盖式更新）。
 *
 * <p>tags 为空数组表示清空全部标签；元素为标签文本（资产标签表 tag 列
 * varchar(64)，数量上限 50）。</p>
 */
@Getter
@Setter
public class AssetTagUpdateCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 标签列表（覆盖式全量替换） */
    @Size(max = 50, message = "标签数量不能超过 50 个")
    private List<@NotBlank(message = "标签不能为空") @Size(max = 64, message = "单个标签长度不能超过 64 字符") String> tags;
}
