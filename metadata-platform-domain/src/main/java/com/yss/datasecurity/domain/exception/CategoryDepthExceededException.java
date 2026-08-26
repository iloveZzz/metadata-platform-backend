package com.yss.datasecurity.domain.exception;

public class CategoryDepthExceededException extends DataSecurityException {
    public CategoryDepthExceededException(int currentDepth) {
        super("CATEGORY_DEPTH_EXCEEDED", 
            String.format("分类目录层级超过最大限制（当前深度: %d，系统最大允许 10 级）！", currentDepth));
    }
}
