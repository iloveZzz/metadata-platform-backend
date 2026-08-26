package com.yss.datamiddle.semantic.term.exception;

/**
 * 无写操作权限（HTTP 403，冻结契约 Forbidden 响应，code = PERMISSION_DENIED）。
 *
 * <p>只读用户（工程师）直调写接口的能力标识兜底（SB-08 / CT-10）；权限横切由 SL-SLICE-06
 * 落位，本切片以 Application 层可测 seam 承载。</p>
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
