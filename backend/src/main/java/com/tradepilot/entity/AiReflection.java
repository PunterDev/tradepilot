package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reflections", indexes = {@Index(name = "idx_ai_reflections_user_id", columnList = "user_id"), @Index(name = "idx_ai_reflections_user_period", columnList = "user_id, start_date, end_date")})
public class AiReflection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "prompt_text", columnDefinition = "text")
    private String promptText;
    @Column(name = "response_text", nullable = false, columnDefinition = "text")
    private String responseText;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class AiReflectionBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private LocalDate startDate;
        @java.lang.SuppressWarnings("all")
        private LocalDate endDate;
        @java.lang.SuppressWarnings("all")
        private String promptText;
        @java.lang.SuppressWarnings("all")
        private String responseText;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AiReflectionBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder startDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder endDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder promptText(final String promptText) {
            this.promptText = promptText;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder responseText(final String responseText) {
            this.responseText = responseText;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiReflection.AiReflectionBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiReflection build() {
            return new AiReflection(this.id, this.user, this.startDate, this.endDate, this.promptText, this.responseText, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiReflection.AiReflectionBuilder(id=" + this.id + ", user=" + this.user + ", startDate=" + this.startDate + ", endDate=" + this.endDate + ", promptText=" + this.promptText + ", responseText=" + this.responseText + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiReflection.AiReflectionBuilder builder() {
        return new AiReflection.AiReflectionBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public User getUser() {
        return this.user;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getStartDate() {
        return this.startDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getEndDate() {
        return this.endDate;
    }

    @java.lang.SuppressWarnings("all")
    public String getPromptText() {
        return this.promptText;
    }

    @java.lang.SuppressWarnings("all")
    public String getResponseText() {
        return this.responseText;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setUser(final User user) {
        this.user = user;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setPromptText(final String promptText) {
        this.promptText = promptText;
    }

    @java.lang.SuppressWarnings("all")
    public void setResponseText(final String responseText) {
        this.responseText = responseText;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiReflection() {
    }

    @java.lang.SuppressWarnings("all")
    public AiReflection(final Long id, final User user, final LocalDate startDate, final LocalDate endDate, final String promptText, final String responseText, final LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.promptText = promptText;
        this.responseText = responseText;
        this.createdAt = createdAt;
    }
}
