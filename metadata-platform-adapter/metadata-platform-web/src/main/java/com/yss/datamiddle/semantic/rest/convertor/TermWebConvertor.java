package com.yss.datamiddle.semantic.rest.convertor;

import com.yss.datamiddle.semantic.application.model.TermCertifyInput;
import com.yss.datamiddle.semantic.application.model.TermCreateInput;
import com.yss.datamiddle.semantic.application.model.TermUpdateInput;
import com.yss.datamiddle.semantic.client.dto.cmd.TermCertifyCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.TermCreateCmd;
import com.yss.datamiddle.semantic.client.dto.cmd.TermUpdateCmd;
import com.yss.datamiddle.semantic.client.vo.TermDetailVO;
import com.yss.datamiddle.semantic.client.vo.TermVO;
import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import com.yss.datamiddle.semantic.term.model.CertifyAction;
import com.yss.datamiddle.semantic.term.model.Term;
import com.yss.datamiddle.semantic.term.model.TermStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 术语 Web 转换器（CMD → Application 输入；Domain → VO；MapStruct，Spring 组件模型）。
 */
@Mapper(componentModel = "spring")
public interface TermWebConvertor {

    TermCreateInput toCreateInput(TermCreateCmd cmd);

    TermUpdateInput toUpdateInput(TermUpdateCmd cmd);

    @Mapping(target = "action", source = "cmd.action")
    TermCertifyInput toCertifyInput(TermCertifyCmd cmd);

    TermVO toVO(Term term);

    List<TermVO> toVOList(List<Term> terms);

    @Mapping(target = "synonymSet", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    TermDetailVO toDetailVO(Term term);

    /**
     * 动作字符串（certify / deprecate）→ 枚举；非法值抛 422 INVALID_ENUM。
     */
    default CertifyAction toCertifyAction(String code) {
        if (code == null) {
            return null;
        }
        try {
            return CertifyAction.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "action",
                    "INVALID_ENUM", "认证动作非法，仅支持 certify / deprecate");
        }
    }

    /**
     * 状态枚举 → 契约字符串。
     */
    default String fromStatus(TermStatus status) {
        return status == null ? null : status.getCode();
    }
}
