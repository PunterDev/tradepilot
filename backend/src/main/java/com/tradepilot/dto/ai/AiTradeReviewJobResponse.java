package com.tradepilot.dto.ai;

public class AiTradeReviewJobResponse {
    private Long reviewId;
    private Long tradeId;
    private String status;
    private AiTradeReviewResponse review;
    private String message;

    @java.lang.SuppressWarnings("all")
    AiTradeReviewJobResponse(final Long reviewId, final Long tradeId, final String status, final AiTradeReviewResponse review, final String message) {
        this.reviewId = reviewId;
        this.tradeId = tradeId;
        this.status = status;
        this.review = review;
        this.message = message;
    }


    @java.lang.SuppressWarnings("all")
    public static class AiTradeReviewJobResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long reviewId;
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private AiTradeReviewResponse review;
        @java.lang.SuppressWarnings("all")
        private String message;

        @java.lang.SuppressWarnings("all")
        AiTradeReviewJobResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder reviewId(final Long reviewId) {
            this.reviewId = reviewId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder review(final AiTradeReviewResponse review) {
            this.review = review;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder message(final String message) {
            this.message = message;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiTradeReviewJobResponse build() {
            return new AiTradeReviewJobResponse(this.reviewId, this.tradeId, this.status, this.review, this.message);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder(reviewId=" + this.reviewId + ", tradeId=" + this.tradeId + ", status=" + this.status + ", review=" + this.review + ", message=" + this.message + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder builder() {
        return new AiTradeReviewJobResponse.AiTradeReviewJobResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getReviewId() {
        return this.reviewId;
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
    public AiTradeReviewResponse getReview() {
        return this.review;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewId(final Long reviewId) {
        this.reviewId = reviewId;
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
    public void setReview(final AiTradeReviewResponse review) {
        this.review = review;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTradeReviewJobResponse)) return false;
        final AiTradeReviewJobResponse other = (AiTradeReviewJobResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$reviewId = this.getReviewId();
        final java.lang.Object other$reviewId = other.getReviewId();
        if (this$reviewId == null ? other$reviewId != null : !this$reviewId.equals(other$reviewId)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$review = this.getReview();
        final java.lang.Object other$review = other.getReview();
        if (this$review == null ? other$review != null : !this$review.equals(other$review)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTradeReviewJobResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $reviewId = this.getReviewId();
        result = result * PRIME + ($reviewId == null ? 43 : $reviewId.hashCode());
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $review = this.getReview();
        result = result * PRIME + ($review == null ? 43 : $review.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTradeReviewJobResponse(reviewId=" + this.getReviewId() + ", tradeId=" + this.getTradeId() + ", status=" + this.getStatus() + ", review=" + this.getReview() + ", message=" + this.getMessage() + ")";
    }
}
