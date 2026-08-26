package com.yss.datamiddle.semantic.client.dto.cmd;

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
public class SynonymRecommendationCmd implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标词不能为空")
    private String targetWord;

    private Integer limit;
}
