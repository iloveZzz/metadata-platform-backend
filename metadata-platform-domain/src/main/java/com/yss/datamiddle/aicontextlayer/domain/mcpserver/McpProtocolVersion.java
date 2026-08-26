package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

/**
 * MCP 接入版本与 protocolVersion 协商（冻结契约第 11 节 SB-01）。
 *
 * <p>Freeze 锁定接入 revision 方向为 {@code 2025-06-18}（MCP 规范 2025-06-18 revision
 * 方向）与 stdio / Streamable HTTP 传输子集；客户端缺省请求回退到支持版本，
 * 不兼容版本协商失败（由适配层映射 invalid_params）。版本变更走冻结后变更流程。</p>
 */
public final class McpProtocolVersion {

    /** 冻结支持的 protocolVersion（契约第 11 节 SB-01，Freeze 批准时锁定方向）。 */
    public static final String SUPPORTED = "2025-06-18";

    private McpProtocolVersion() {
    }

    /**
     * 协商 protocolVersion。
     *
     * @param requested 客户端请求的版本；null 或空白按缺省处理（回退到支持版本）
     * @return 协商结果
     */
    public static NegotiationResult negotiate(String requested) {
        if (requested == null || requested.trim().isEmpty()) {
            return NegotiationResult.accepted(SUPPORTED);
        }
        String normalized = requested.trim();
        if (SUPPORTED.equals(normalized)) {
            return NegotiationResult.accepted(SUPPORTED);
        }
        return NegotiationResult.rejected(normalized);
    }

    /**
     * protocolVersion 协商结果。
     */
    public static final class NegotiationResult {

        private final boolean accepted;
        private final String requested;
        private final String negotiated;

        private NegotiationResult(boolean accepted, String requested, String negotiated) {
            this.accepted = accepted;
            this.requested = requested;
            this.negotiated = negotiated;
        }

        static NegotiationResult accepted(String negotiated) {
            return new NegotiationResult(true, null, negotiated);
        }

        static NegotiationResult rejected(String requested) {
            return new NegotiationResult(false, requested, null);
        }

        public boolean isAccepted() {
            return accepted;
        }

        /**
         * 客户端请求的版本（协商失败时使用）。
         */
        public String getRequested() {
            return requested;
        }

        /**
         * 协商成功的版本（成功时使用）。
         */
        public String getNegotiated() {
            return negotiated;
        }
    }
}
