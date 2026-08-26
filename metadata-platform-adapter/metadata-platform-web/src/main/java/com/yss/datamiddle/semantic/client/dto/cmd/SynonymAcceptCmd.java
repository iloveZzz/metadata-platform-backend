package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynonymAcceptCmd extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "同义词组 ID 不能为空")
    private Long synonymSetId;

    @NotBlank(message = "采纳词条不能为空")
    private String candidateWord;
}
