package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricBatchImportCmd extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "CSV 内容不能为空")
    private String csvContent;

    private Boolean overwriteExisting;
}
