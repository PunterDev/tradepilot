package com.tradepilot.dto.ai;

public class AiTradeAnalysisJobResponse {
    private Long analysisId;
    private Long tradeId;
    private String status;
    private AiTradeAnalysisResponse analysis;
    private String message;

    @java.lang.SuppressWarnings("all")
    AiTradeAnalysisJobResponse(final Long analysisId, final Long tradeId, final String status, final AiTradeAnalysisResponse analysis, final String message) {
        this.analysisId = analysisId;
        this.tradeId = tradeId;
        this.status = status;
        this.analysis = analysis;
        this.message = message;
    }


    @java.lang.SuppressWarnings("all")
    public static class AiTradeAnalysisJobResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long analysisId;
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private AiTradeAnalysisResponse analysis;
        @java.lang.SuppressWarnings("all")
        private String message;

        @java.lang.SuppressWarnings("all")
        AiTradeAnalysisJobResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder analysisId(final Long analysisId) {
            this.analysisId = analysisId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder analysis(final AiTradeAnalysisResponse analysis) {
            this.analysis = analysis;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder message(final String message) {
            this.message = message;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiTradeAnalysisJobResponse build() {
            return new AiTradeAnalysisJobResponse(this.analysisId, this.tradeId, this.status, this.analysis, this.message);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder(analysisId=" + this.analysisId + ", tradeId=" + this.tradeId + ", status=" + this.status + ", analysis=" + this.analysis + ", message=" + this.message + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder builder() {
        return new AiTradeAnalysisJobResponse.AiTradeAnalysisJobResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getAnalysisId() {
        return this.analysisId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisResponse getAnalysis() {
        return this.analysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public void setAnalysisId(final Long analysisId) {
        this.analysisId = analysisId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setAnalysis(final AiTradeAnalysisResponse analysis) {
        this.analysis = analysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTradeAnalysisJobResponse)) return false;
        final AiTradeAnalysisJobResponse other = (AiTradeAnalysisJobResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$analysisId = this.getAnalysisId();
        final java.lang.Object other$analysisId = other.getAnalysisId();
        if (this$analysisId == null ? other$analysisId != null : !this$analysisId.equals(other$analysisId)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$analysis = this.getAnalysis();
        final java.lang.Object other$analysis = other.getAnalysis();
        if (this$analysis == null ? other$analysis != null : !this$analysis.equals(other$analysis)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTradeAnalysisJobResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $analysisId = this.getAnalysisId();
        result = result * PRIME + ($analysisId == null ? 43 : $analysisId.hashCode());
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $analysis = this.getAnalysis();
        result = result * PRIME + ($analysis == null ? 43 : $analysis.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTradeAnalysisJobResponse(analysisId=" + this.getAnalysisId() + ", tradeId=" + this.getTradeId() + ", status=" + this.getStatus() + ", analysis=" + this.getAnalysis() + ", message=" + this.getMessage() + ")";
    }
}
