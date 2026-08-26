package com.yss.datamiddle.semantic.application.model;

import com.yss.datamiddle.semantic.term.model.CertifyAction;
import lombok.Builder;
import lombok.Getter;

/**
 * 认证 / 弃用用例输入。
 */
@Getter
@Builder
public class TermCertifyInput {

    private final CertifyAction action;

    /** 操作备注（写入审计） */
    private final String note;
}
