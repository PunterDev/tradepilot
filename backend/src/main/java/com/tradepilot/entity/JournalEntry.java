package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries", indexes = {@Index(name = "idx_journal_entries_user_id", columnList = "user_id"), @Index(name = "idx_journal_entries_user_account_date", columnList = "user_id, account_id, journal_date")}, uniqueConstraints = {@UniqueConstraint(name = "uk_journal_user_account_date", columnNames = {"user_id", "account_id", "journal_date"})})
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private User user;
    @ManyToOne
    private TradingAccount account;
    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;
    @Column(length = 50)
    private String mood;
    @Column(name = "setup_quality", length = 100)
    private String setupQuality;
    @Column(length = 1000)
    private String mistakes;
    @Column(name = "lesson_learned", length = 1000)
    private String lessonLearned;
    @Column(length = 2000)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class JournalEntryBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private TradingAccount account;
        @java.lang.SuppressWarnings("all")
        private LocalDate journalDate;
        @java.lang.SuppressWarnings("all")
        private String mood;
        @java.lang.SuppressWarnings("all")
        private String setupQuality;
        @java.lang.SuppressWarnings("all")
        private String mistakes;
        @java.lang.SuppressWarnings("all")
        private String lessonLearned;
        @java.lang.SuppressWarnings("all")
        private String notes;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        JournalEntryBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder account(final TradingAccount account) {
            this.account = account;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder journalDate(final LocalDate journalDate) {
            this.journalDate = journalDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder mood(final String mood) {
            this.mood = mood;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder setupQuality(final String setupQuality) {
            this.setupQuality = setupQuality;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder mistakes(final String mistakes) {
            this.mistakes = mistakes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder lessonLearned(final String lessonLearned) {
            this.lessonLearned = lessonLearned;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder notes(final String notes) {
            this.notes = notes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public JournalEntry.JournalEntryBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JournalEntry build() {
            return new JournalEntry(this.id, this.user, this.account, this.journalDate, this.mood, this.setupQuality, this.mistakes, this.lessonLearned, this.notes, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "JournalEntry.JournalEntryBuilder(id=" + this.id + ", user=" + this.user + ", account=" + this.account + ", journalDate=" + this.journalDate + ", mood=" + this.mood + ", setupQuality=" + this.setupQuality + ", mistakes=" + this.mistakes + ", lessonLearned=" + this.lessonLearned + ", notes=" + this.notes + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static JournalEntry.JournalEntryBuilder builder() {
        return new JournalEntry.JournalEntryBuilder();
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
    public TradingAccount getAccount() {
        return this.account;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getJournalDate() {
        return this.journalDate;
    }

    @java.lang.SuppressWarnings("all")
    public String getMood() {
        return this.mood;
    }

    @java.lang.SuppressWarnings("all")
    public String getSetupQuality() {
        return this.setupQuality;
    }

    @java.lang.SuppressWarnings("all")
    public String getMistakes() {
        return this.mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public String getLessonLearned() {
        return this.lessonLearned;
    }

    @java.lang.SuppressWarnings("all")
    public String getNotes() {
        return this.notes;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
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
    public void setAccount(final TradingAccount account) {
        this.account = account;
    }

    @java.lang.SuppressWarnings("all")
    public void setJournalDate(final LocalDate journalDate) {
        this.journalDate = journalDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setMood(final String mood) {
        this.mood = mood;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetupQuality(final String setupQuality) {
        this.setupQuality = setupQuality;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakes(final String mistakes) {
        this.mistakes = mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setLessonLearned(final String lessonLearned) {
        this.lessonLearned = lessonLearned;
    }

    @java.lang.SuppressWarnings("all")
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public JournalEntry() {
    }

    @java.lang.SuppressWarnings("all")
    public JournalEntry(final Long id, final User user, final TradingAccount account, final LocalDate journalDate, final String mood, final String setupQuality, final String mistakes, final String lessonLearned, final String notes, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.account = account;
        this.journalDate = journalDate;
        this.mood = mood;
        this.setupQuality = setupQuality;
        this.mistakes = mistakes;
        this.lessonLearned = lessonLearned;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
