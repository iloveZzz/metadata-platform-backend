package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * 确认/修正候选分类命令（POST /api/classifications/{id}/confirm 可选 body）。
 *
 * <p>冻结 spec 未声明 requestBody（偏离登记：确认幂等 + 修正语义合一）；
 * correctedName 为空 = 确认候选；非空 = 以修正名覆盖并流转已修正。</p>
 */
@Getter
@Setter
public class ClassificationConfirmCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 修正后的分类名（可选；为空表示确认候选分类） */
    private String correctedName;
}
